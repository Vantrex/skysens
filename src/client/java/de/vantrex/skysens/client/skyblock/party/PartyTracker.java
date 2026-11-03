package de.vantrex.skysens.client.skyblock.party;

import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.skyblock.event.PartyJoinEvent;
import de.vantrex.skysens.client.skyblock.event.PartyKickEvent;
import de.vantrex.skysens.client.skyblock.event.PartyLeaveEvent;
import de.vantrex.skysens.client.skyblock.event.PartyMemberJoinEvent;
import de.vantrex.skysens.client.skyblock.event.PartyMemberLeaveEvent;
import de.vantrex.skysens.client.skyblock.event.PartyMembersUpdateEvent;
import de.vantrex.skysens.client.skyblock.event.PartyRoleChangeEvent;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the player's Hypixel party: whether they are in one, the other members, everyone's {@link PartyRole rank} and
 * the size.
 * <p>
 * Once per connection the state is read from the Hypixel Mod API ({@link PartyService}), as are the answers to requests
 * of other mods; this also picks up a party the player was already in when joining Hypixel. After that, the party chat
 * messages keep it up to date ("You have joined ReZuni's party!", "ReZuni joined the party.", "ReZuni has promoted
 * FaiN69 to Party Moderator", ...), and {@code /party list} re-reads the whole party. The Mod API only names members in
 * the same lobby, so a member's name can be unknown; a rank is unknown for the members listed when joining a party.
 * <p>
 * The state is not saved: it is unknown after a restart or reconnect until the Mod API answered or a party message was
 * seen. Always active, so the events fire regardless of which features are on. All state changes run on the client
 * thread. Fires {@link PartyJoinEvent}, {@link PartyLeaveEvent}, {@link PartyKickEvent}, {@link PartyMemberJoinEvent},
 * {@link PartyMemberLeaveEvent}, {@link PartyRoleChangeEvent} and {@link PartyMembersUpdateEvent}.
 */
public final class PartyTracker implements GameMessageListeningFeature {

    /** An optional rank prefix like "[MVP+] ", then the name. */
    private static final String NAME = "(?:\\[[^]]+] )?(\\w{1,16})";

    private static final Pattern SELF_JOINED = Pattern.compile("^You have joined " + NAME + "'s? party!$");
    private static final Pattern PARTYING_WITH = Pattern.compile("^You'll be partying with: (.+)$");
    private static final Pattern INVITED = Pattern.compile("^" + NAME + " invited " + NAME + " to the party! They have \\d+ seconds to accept\\.$");
    private static final Pattern MEMBER_JOINED = Pattern.compile("^" + NAME + " joined the party\\.$");
    private static final Pattern MEMBER_LEFT = Pattern.compile("^" + NAME + " has left the party\\.$");
    private static final Pattern MEMBER_KICKED = Pattern.compile("^" + NAME + " has been removed from the party\\.$");
    private static final Pattern MEMBER_DISCONNECTED = Pattern.compile("^" + NAME + " was removed from your party because they disconnected\\.?$");
    private static final Pattern MEMBER_KICKED_OFFLINE = Pattern.compile("^Kicked " + NAME + " because they were offline\\.?$");
    private static final Pattern SELF_LEFT = Pattern.compile("^You left the party\\.$");
    private static final Pattern SELF_KICKED = Pattern.compile("^You have been kicked from the party by " + NAME + "\\.?$");
    private static final Pattern DISBANDED = Pattern.compile("^(?:" + NAME + " has disbanded the party!|The party was disbanded because .+)$");
    private static final Pattern NOT_IN_PARTY = Pattern.compile("^You are not (?:currently )?in a party(?: right now)?\\.$");
    /** /party transfer, the old leader becomes a moderator. */
    private static final Pattern TRANSFERRED_BY = Pattern.compile("^The party was transferred to " + NAME + " by " + NAME + "$");
    /** E.g. "... because ReZuni left", the old leader is gone. */
    private static final Pattern TRANSFERRED_BECAUSE = Pattern.compile("^The party was transferred to " + NAME + " because .+$");
    private static final Pattern ROLE_CHANGED = Pattern.compile("^" + NAME + " has (?:promoted|demoted) " + NAME + " to Party (Leader|Moderator|Member)$");

    // /party list, one multi-line message
    private static final Pattern LIST_HEADER = Pattern.compile("^Party Members \\((\\d+)\\)$");
    private static final Pattern LIST_ROLE = Pattern.compile("^Party (Leader|Moderators|Members): (.*)$");
    /** A member in a /party list role line, each followed by an online dot. */
    private static final Pattern LIST_MEMBER = Pattern.compile(NAME + " ●");
    private static final Pattern NAME_IN_LIST = Pattern.compile(NAME);
    private static final Pattern SEPARATOR = Pattern.compile("^[-▬=]+$");

    @Getter
    private static final PartyTracker instance = new PartyTracker();

    /** False until the Mod API answered or a party message told us, and again after a disconnect. */
    @Getter
    private boolean known;
    private boolean inParty;
    /** The player's own rank, null if unknown or not in a party. */
    private @Nullable PartyRole role;
    /** The other members, never the player. */
    private final List<PartyMember> members = new ArrayList<>();
    /** Whether the Mod API was already asked on this connection, it rate limits party info requests. */
    private boolean requested;

    private PartyTracker() {
        // registered before PartyService's handler, so our state is updated when its futures complete
        final HypixelModAPI api = HypixelModAPI.getInstance();
        api.createHandler(ClientboundPartyInfoPacket.class, packet -> Minecraft.getInstance().execute(() -> this.onPartyInfo(packet)));
        api.createHandler(ClientboundLocationPacket.class, packet -> Minecraft.getInstance().execute(this::requestOnce));
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (!overlay) {
            this.onChatMessage(message.getString());
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    // ---- queries ----

    /** Whether the player is in a party, false if {@link #isKnown() unknown}. */
    public boolean isInParty() {
        return this.known && this.inParty;
    }

    /** Whether the player is in a party, empty if unknown. */
    public @NotNull Optional<Boolean> getInParty() {
        return this.known ? Optional.of(this.inParty) : Optional.empty();
    }

    /**
     * Asks Hypixel via the Mod API whether we are in a party and updates the tracker from the answer. Use it when acting
     * on a wrong cached state would be harmful. Completes on the client thread; empty if Hypixel did not answer in time.
     */
    public @NotNull CompletableFuture<Optional<Boolean>> checkInParty() {
        return PartyService.getInstance().isInParty();
    }

    /** The player's own rank, null if unknown or not in a party. */
    public @Nullable PartyRole getRole() {
        return this.isInParty() ? this.role : null;
    }

    /** The rank of the given player (the player themselves included), null if unknown or not in our party. */
    public @Nullable PartyRole getRole(@NotNull String name) {
        if (name.equalsIgnoreCase(selfName())) {
            return this.getRole();
        }
        return this.isInParty() ? this.findMember(name).map(PartyMember::getRole).orElse(null) : null;
    }

    /** Whether the player leads their party. */
    public boolean isLeader() {
        return this.getRole() == PartyRole.LEADER;
    }

    /** Whether the player's rank is the given one or higher, e.g. {@code hasRole(MODERATOR)} to kick. */
    public boolean hasRole(@NotNull PartyRole required) {
        final PartyRole own = this.getRole();
        return own != null && own.isAtLeast(required);
    }

    /** Whether another member is known to lead the party. */
    public boolean isLedByOtherMember() {
        return this.isInParty() && this.members.stream().anyMatch(member -> member.getRole() == PartyRole.LEADER);
    }

    /** The leader's name, the player's own if they lead. Null if unknown or not in a party. */
    public @Nullable String getLeader() {
        if (!this.isInParty()) {
            return null;
        }
        if (this.role == PartyRole.LEADER) {
            return selfName();
        }
        return this.members.stream()
                .filter(member -> member.getRole() == PartyRole.LEADER)
                .map(PartyMember::getName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The other members, without the player. Empty if not in a party. */
    public @NotNull List<PartyMember> getMembers() {
        return Collections.unmodifiableList(this.members);
    }

    /** The names of the other members, without the player and without members whose name is unknown. */
    public @NotNull List<String> getMemberNames() {
        return this.members.stream().map(PartyMember::getName).filter(Objects::nonNull).toList();
    }

    /** The party size including the player, 0 if not in a party or unknown. */
    public int getSize() {
        return this.isInParty() ? this.members.size() + 1 : 0;
    }

    /** Whether the given player is another member of our party. */
    public boolean hasMember(@NotNull String name) {
        return this.isInParty() && this.findMember(name).isPresent();
    }

    private @NotNull Optional<PartyMember> findMember(@NotNull String name) {
        return this.members.stream().filter(member -> member.hasName(name)).findFirst();
    }

    // ---- connection ----

    /** Forgets the state, e.g. on disconnect. Fires no events, the party itself survives a reconnect. */
    public void reset() {
        this.known = false;
        this.clearParty();
        this.requested = false;
    }

    private void clearParty() {
        this.inParty = false;
        this.role = null;
        this.members.clear();
    }

    private void requestOnce() {
        if (this.requested || this.known) {
            return;
        }
        this.requested = true;
        // the answer arrives through our packet handler
        PartyService.getInstance().isInParty();
    }

    // ---- Mod API ----

    private void onPartyInfo(@NotNull ClientboundPartyInfoPacket packet) {
        final Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        ClientUtil.sendDebug("Party info: " + packet);
        if (!packet.isInParty()) {
            this.applySnapshot(false, null, List.of());
            return;
        }
        final UUID self = client.player.getUUID();
        final UUID leaderId = packet.getLeader().orElse(null);
        PartyRole ownRole = leaderId != null && leaderId.equals(self) ? PartyRole.LEADER : null;
        final List<PartyMember> snapshot = new ArrayList<>();
        // the member map may or may not contain us, the size is counted without us either way
        for (ClientboundPartyInfoPacket.PartyMember member : packet.getMemberMap().values()) {
            PartyRole memberRole = PartyRole.of(member.getRole());
            if (memberRole == null && member.getUuid().equals(leaderId)) {
                memberRole = PartyRole.LEADER;
            }
            if (member.getUuid().equals(self)) {
                ownRole = memberRole;
            } else {
                snapshot.add(new PartyMember(member.getUuid(), this.nameOf(member.getUuid()), memberRole));
            }
        }
        this.applySnapshot(true, ownRole, snapshot);
    }

    /** The name from the tab list (same lobby), else the name we already know for this UUID, else null. */
    private @Nullable String nameOf(@NotNull UUID uuid) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        final PlayerInfo info = connection == null ? null : connection.getPlayerInfo(uuid);
        if (info != null) {
            return info.getProfile().name();
        }
        return this.members.stream()
                .filter(member -> uuid.equals(member.getUuid()))
                .map(PartyMember::getName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The UUID from the tab list (same lobby), else the UUID we already know for this name, else null. */
    private @Nullable UUID uuidOf(@NotNull String name) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        final PlayerInfo info = connection == null ? null : connection.getPlayerInfo(name);
        if (info != null) {
            return info.getProfile().id();
        }
        return this.findMember(name).map(PartyMember::getUuid).orElse(null);
    }

    /**
     * Replaces the whole state, from the Mod API or /party list. Fires a leave event if we turn out not to be in a party
     * anymore; finding a party we missed joining, or ranks we missed changing, only fires the
     * {@link PartyMembersUpdateEvent}.
     */
    private void applySnapshot(boolean nowInParty, @Nullable PartyRole ownRole, @NotNull List<PartyMember> newMembers) {
        final boolean wasKnown = this.known;
        final boolean wasInParty = this.inParty;
        final boolean changed = !wasKnown || wasInParty != nowInParty
                || this.role != ownRole || !this.members.equals(newMembers);
        this.known = true;
        this.clearParty();
        if (nowInParty) {
            this.inParty = true;
            this.role = ownRole;
            this.members.addAll(newMembers);
        }
        if (wasKnown && wasInParty && !nowInParty) {
            fire(new PartyLeaveEvent(PartyLeaveReason.UNKNOWN));
        }
        if (changed) {
            fire(new PartyMembersUpdateEvent(this.inParty, this.getRole(), this.getLeader(), this.getMemberNames(), this.getSize()));
        }
    }

    // ---- chat ----

    /** Handles one chat message, formatting codes included. Party messages are multi-line, wrapped in dashes. */
    public void onChatMessage(@NotNull String rawMessage) {
        final String self = selfName();
        if (self == null) {
            return;
        }
        final List<String> lines = new ArrayList<>();
        for (String line : ChatUtil.strip(rawMessage).split("\n")) {
            final String stripped = line.trim();
            if (!stripped.isEmpty() && !SEPARATOR.matcher(stripped).matches()) {
                lines.add(stripped);
            }
        }
        // matched before normalizing, which would cut the "(3)" off the header
        if (!lines.isEmpty() && LIST_HEADER.matcher(lines.getFirst()).matches()) {
            this.onPartyList(lines, self);
            return;
        }
        for (String line : lines) {
            this.onChatLine(ChatUtil.normalize(line), self);
        }
    }

    private void onChatLine(@NotNull String line, @NotNull String self) {
        Matcher matcher;
        if ((matcher = SELF_JOINED.matcher(line)).matches()) {
            this.onSelfJoined(matcher.group(1));
        } else if ((matcher = PARTYING_WITH.matcher(line)).matches()) {
            this.onPartyingWith(matcher.group(1), self);
        } else if ((matcher = INVITED.matcher(line)).matches()) {
            this.onInvited(matcher.group(1), self);
        } else if ((matcher = MEMBER_JOINED.matcher(line)).matches()) {
            this.onMemberJoined(matcher.group(1), self);
        } else if ((matcher = MEMBER_LEFT.matcher(line)).matches()) {
            this.onMemberLeft(matcher.group(1), PartyLeaveReason.LEFT, self);
        } else if ((matcher = MEMBER_KICKED.matcher(line)).matches()) {
            this.onMemberLeft(matcher.group(1), PartyLeaveReason.KICKED, self);
        } else if ((matcher = MEMBER_DISCONNECTED.matcher(line)).matches()
                || (matcher = MEMBER_KICKED_OFFLINE.matcher(line)).matches()) {
            this.onMemberLeft(matcher.group(1), PartyLeaveReason.DISCONNECTED, self);
        } else if (SELF_LEFT.matcher(line).matches()) {
            this.onSelfLeft(PartyLeaveReason.LEFT, true);
        } else if ((matcher = SELF_KICKED.matcher(line)).matches()) {
            fire(new PartyKickEvent(matcher.group(1)));
            this.onSelfLeft(PartyLeaveReason.KICKED, true);
        } else if (DISBANDED.matcher(line).matches()) {
            this.onSelfLeft(PartyLeaveReason.DISBANDED, true);
        } else if (NOT_IN_PARTY.matcher(line).matches()) {
            this.onSelfLeft(PartyLeaveReason.UNKNOWN, false);
        } else if ((matcher = TRANSFERRED_BY.matcher(line)).matches()) {
            this.onNewLeader(matcher.group(1), true, self);
        } else if ((matcher = TRANSFERRED_BECAUSE.matcher(line)).matches()) {
            this.onNewLeader(matcher.group(1), false, self);
        } else if ((matcher = ROLE_CHANGED.matcher(line)).matches()) {
            final PartyRole newRole = Objects.requireNonNull(PartyRole.ofTitle(matcher.group(3)));
            if (newRole == PartyRole.LEADER) {
                this.onNewLeader(matcher.group(2), true, self);
            } else {
                this.changeRole(matcher.group(2), newRole, self);
            }
        }
    }

    private void onSelfJoined(@NotNull String leader) {
        this.clearParty();
        this.known = true;
        this.inParty = true;
        this.role = PartyRole.MEMBER;
        this.members.add(new PartyMember(this.uuidOf(leader), leader, PartyRole.LEADER));
        ClientUtil.sendDebug("Party: joined " + leader + "'s party");
        fire(new PartyJoinEvent(leader, this.getMemberNames()));
    }

    /** Follows "You have joined ...'s party!" and lists the members already in it, their ranks are unknown. */
    private void onPartyingWith(@NotNull String list, @NotNull String self) {
        for (String entry : list.split(", ")) {
            final Matcher name = NAME_IN_LIST.matcher(entry.trim());
            if (name.matches()) {
                this.addMember(name.group(1), null, self);
            }
        }
    }

    /** An invite by us creates a party with us as the leader if we weren't in one. */
    private void onInvited(@NotNull String inviter, @NotNull String self) {
        if (!inviter.equalsIgnoreCase(self) || this.isInParty()) {
            return;
        }
        final boolean wasKnown = this.known;
        this.clearParty();
        this.known = true;
        this.inParty = true;
        if (!wasKnown) {
            // we may have been in someone else's party all along (members can invite too), so our rank is unknown
            return;
        }
        this.role = PartyRole.LEADER;
        ClientUtil.sendDebug("Party: created a party");
        fire(new PartyJoinEvent(self, List.of()));
    }

    private void onMemberJoined(@NotNull String name, @NotNull String self) {
        if (name.equalsIgnoreCase(self)) {
            return;
        }
        if (!this.isInParty()) {
            // only shown to party members, so we are in one; our rank is unknown if we missed how we got here
            this.clearParty();
            this.known = true;
            this.inParty = true;
        }
        this.addMember(name, PartyRole.MEMBER, self);
        ClientUtil.sendDebug("Party: " + name + " joined");
        fire(new PartyMemberJoinEvent(name));
    }

    /** Adds the member if they aren't known yet; a known member keeps their rank unless it was unknown. */
    private @Nullable PartyMember addMember(@NotNull String name, @Nullable PartyRole memberRole, @NotNull String self) {
        if (name.equalsIgnoreCase(self)) {
            return null;
        }
        final Optional<PartyMember> existing = this.findMember(name);
        if (existing.isPresent()) {
            if (existing.get().getRole() == null) {
                existing.get().setRole(memberRole);
            }
            return existing.get();
        }
        final PartyMember member = new PartyMember(this.uuidOf(name), name, memberRole);
        this.members.add(member);
        return member;
    }

    private void onMemberLeft(@NotNull String name, @NotNull PartyLeaveReason reason, @NotNull String self) {
        if (name.equalsIgnoreCase(self)) {
            return;
        }
        this.members.removeIf(member -> member.hasName(name));
        ClientUtil.sendDebug("Party: " + name + " left (" + reason + ")");
        fire(new PartyMemberLeaveEvent(name, reason));
    }

    /** @param certain false if the message doesn't prove we were in a party, then only a tracked party fires the event */
    private void onSelfLeft(@NotNull PartyLeaveReason reason, boolean certain) {
        final boolean fireEvent = certain || this.isInParty();
        this.clearParty();
        this.known = true;
        if (fireEvent) {
            ClientUtil.sendDebug("Party: left (" + reason + ")");
            fire(new PartyLeaveEvent(reason));
        }
    }

    /**
     * The party now belongs to {@code name}.
     *
     * @param oldLeaderStays whether the old leader is still in the party, then they become a moderator
     */
    private void onNewLeader(@NotNull String name, boolean oldLeaderStays, @NotNull String self) {
        if (!this.isInParty()) {
            return;
        }
        if (this.role == PartyRole.LEADER && !name.equalsIgnoreCase(self)) {
            this.changeRole(self, oldLeaderStays ? PartyRole.MODERATOR : PartyRole.MEMBER, self);
        }
        for (PartyMember member : List.copyOf(this.members)) {
            if (member.getRole() == PartyRole.LEADER && !member.hasName(name)) {
                if (oldLeaderStays && member.getName() != null) {
                    this.changeRole(member.getName(), PartyRole.MODERATOR, self);
                } else {
                    // gone, or no name to report; the leave message removes them
                    member.setRole(PartyRole.MODERATOR);
                }
            }
        }
        this.changeRole(name, PartyRole.LEADER, self);
    }

    /** Sets the rank of a member or the player and fires a {@link PartyRoleChangeEvent} if it changed. */
    private void changeRole(@NotNull String name, @NotNull PartyRole newRole, @NotNull String self) {
        if (!this.isInParty()) {
            return;
        }
        final boolean isSelf = name.equalsIgnoreCase(self);
        final PartyRole oldRole;
        if (isSelf) {
            oldRole = this.role;
            this.role = newRole;
        } else {
            final PartyMember existing = this.findMember(name).orElse(null);
            oldRole = existing == null ? null : existing.getRole();
            final PartyMember member = existing != null ? existing : Objects.requireNonNull(this.addMember(name, newRole, self));
            member.setRole(newRole);
        }
        if (oldRole != newRole) {
            ClientUtil.sendDebug("Party: " + name + " is now " + newRole);
            fire(new PartyRoleChangeEvent(name, isSelf, oldRole, newRole));
        }
    }

    /**
     * Re-reads the party from the {@code /party list} output:
     * <pre>
     * Party Members (3)
     * Party Leader: [MVP+] Vantrex ●
     * Party Moderators: [VIP] ReZuni ●
     * Party Members: FaiN69 ● [MVP] Foo ●
     * </pre>
     */
    private void onPartyList(@NotNull List<String> lines, @NotNull String self) {
        boolean sawLeader = false;
        PartyRole ownRole = null;
        final List<PartyMember> snapshot = new ArrayList<>();
        for (String line : lines) {
            final Matcher roleLine = LIST_ROLE.matcher(line);
            if (!roleLine.matches()) {
                continue;
            }
            final PartyRole lineRole = Objects.requireNonNull(PartyRole.ofTitle(roleLine.group(1)));
            sawLeader |= lineRole == PartyRole.LEADER;
            final Matcher member = LIST_MEMBER.matcher(roleLine.group(2));
            while (member.find()) {
                final String name = member.group(1);
                if (name.equalsIgnoreCase(self)) {
                    ownRole = lineRole;
                } else if (snapshot.stream().noneMatch(known -> known.hasName(name))) {
                    snapshot.add(new PartyMember(this.uuidOf(name), name, lineRole));
                }
            }
        }
        if (!sawLeader) {
            // the role lines came in another message, don't wipe the members with an empty list
            return;
        }
        this.applySnapshot(true, ownRole, snapshot);
    }

    private static @Nullable String selfName() {
        final var player = Minecraft.getInstance().player;
        return player == null ? null : player.getName().getString();
    }

    private static void fire(@NotNull SkysensEvent event) {
        EventListenerRegistry.REGISTRY.fireEvent(event);
    }

}
