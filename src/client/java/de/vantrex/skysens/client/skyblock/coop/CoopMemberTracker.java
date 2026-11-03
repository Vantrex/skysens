package de.vantrex.skysens.client.skyblock.coop;

import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import de.vantrex.skysens.client.skyblock.coop.CoopRepository.Coop;
import de.vantrex.skysens.client.skyblock.coop.CoopRepository.Member;
import de.vantrex.skysens.client.skyblock.event.CoopMemberJoinEvent;
import de.vantrex.skysens.client.skyblock.event.CoopMemberQuitEvent;
import de.vantrex.skysens.client.skyblock.event.CoopMembersUpdateEvent;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.player.ProfileService;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.TabListUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads the co-op members from the "Coop (n)" tab list widget, which Hypixel only shows on the private island:
 * <pre>
 * Coop (3)
 * [470] Vantrex ♲
 * ReZuni
 * FaiN69 (Offline 12d)
 * </pre>
 * The tab list is only read on the private island; on a visited island the widget would list the host's co-op, so it is
 * also only taken if it lists us. Away from the island, the "ReZuni joined SkyBlock." and "ReZuni left SkyBlock."
 * chat messages keep the online status up to date. The members are cached per profile (by id, by name while the id is
 * unknown) in the {@link CoopRepository}, so they are also known away from the island and after a restart, e.g. for
 * the warp menu. Always active, so the events fire regardless of which features are on. All methods run on the client
 * thread. Fires {@link CoopMemberJoinEvent}, {@link CoopMemberQuitEvent} and
 * {@link CoopMembersUpdateEvent}.
 */
public final class CoopMemberTracker implements AfterTickFeature, GameMessageListeningFeature {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^Co-?op \\((\\d+)\\)$");
    // optional SkyBlock level, the name, then e.g. a symbol or "(Offline 12d)"
    private static final Pattern MEMBER_PATTERN = Pattern.compile("^(?:\\[\\d+] )?(\\w{1,16})(.*)$");
    private static final Pattern OFFLINE_PATTERN = Pattern.compile("\\(Offline(?: ([^)]*))?\\)");
    /** Sent when a co-op member joins or leaves SkyBlock, matched against a stripped, normalized chat line. */
    private static final Pattern JOIN_LEAVE_PATTERN = Pattern.compile("^(\\w{1,16}) (joined|left) SkyBlock\\.$");
    /** Used while the profile name is unknown. */
    private static final String UNKNOWN_PROFILE = "unknown";
    /** How often the tab list is read. */
    private static final int REFRESH_TICKS = 20;

    @Getter
    private static final CoopMemberTracker instance = new CoopMemberTracker();

    /** When the widget was last seen in this session, 0 if not yet. */
    private long liveAt;
    private int ticks;

    private CoopMemberTracker() {
    }

    @Override
    public void afterTick() {
        if (++this.ticks >= REFRESH_TICKS) {
            this.ticks = 0;
            this.refresh();
        }
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (!overlay) {
            this.onChatLine(ChatUtil.normalize(ChatUtil.strip(message.getString())));
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    /** Re-reads the tab list on the private island and caches the members if the widget is shown. */
    public void refresh() {
        final Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || client.player == null) {
            return;
        }
        final CoopRepository repository = RepositoryRegistry.COOP_REPOSITORY;
        final ProfileService profiles = ProfileService.getInstance();
        if (profiles.getCurrentProfile() != null) {
            repository.lastProfile = profiles.getCurrentProfile();
            repository.lastProfileId = profiles.getCurrentProfileId();
        }
        // the widget is only shown on the private island
        if (LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.PRIVATE_ISLAND) {
            return;
        }
        final List<Member> members = parseMembers(TabListUtil.lines(), client.player.getName().getString());
        if (members != null) {
            final Coop coop = this.currentCoop();
            final boolean changed = !coop.members.equals(members);
            coop.members = members;
            coop.lastSeenAt = System.currentTimeMillis();
            this.liveAt = coop.lastSeenAt;
            if (changed) {
                EventListenerRegistry.REGISTRY.fireEvent(new CoopMembersUpdateEvent(
                        members.stream().map(member -> member.name).toList(),
                        members.stream().filter(member -> member.online).map(member -> member.name).collect(Collectors.toSet())));
            }
        }
    }

    /** Updates the online status from a co-op join/leave message, ignores any other line. Takes a stripped, normalized chat line. */
    public void onChatLine(@NotNull String line) {
        final Matcher matcher = JOIN_LEAVE_PATTERN.matcher(line);
        final Minecraft client = Minecraft.getInstance();
        if (!matcher.matches() || client.player == null) {
            return;
        }
        final String name = matcher.group(1);
        if (name.equalsIgnoreCase(client.player.getName().getString())) {
            return;
        }
        final boolean online = matcher.group(2).equals("joined");
        final Coop coop = this.currentCoop();
        final Member member = coop.members.stream()
                .filter(candidate -> candidate.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (member == null) {
            // only co-op members are announced, so this one joined the co-op or the island wasn't visited yet
            coop.members.add(new Member(name, online, null));
        } else {
            member.online = online;
            member.offlineFor = null;
        }
        ClientUtil.sendDebug("Co-op: " + name + (online ? " joined" : " left"));
        EventListenerRegistry.REGISTRY.fireEvent(online ? new CoopMemberJoinEvent(name) : new CoopMemberQuitEvent(name));
    }

    /** The cached co-op of the current profile, null if its members were never read. */
    public @Nullable Coop getCoop() {
        final CoopRepository repository = RepositoryRegistry.COOP_REPOSITORY;
        final ProfileService profiles = ProfileService.getInstance();
        Coop coop = null;
        if (profiles.getCurrentProfile() != null) {
            coop = repository.find(profiles.getCurrentProfileId(), profiles.getCurrentProfile());
        } else if (repository.lastProfile != null) {
            coop = repository.find(repository.lastProfileId, repository.lastProfile);
        }
        return coop != null ? coop : repository.profiles.get(UNKNOWN_PROFILE);
    }

    /** The co-op of the current profile, added if there is none; stored as "unknown" while the profile is. */
    private @NotNull Coop currentCoop() {
        final ProfileService profiles = ProfileService.getInstance();
        final String profile = profiles.getCurrentProfile();
        return RepositoryRegistry.COOP_REPOSITORY.coop(profiles.getCurrentProfileId(), profile == null ? UNKNOWN_PROFILE : profile);
    }

    /** True if the widget is in the tab list right now, so the online status is current. */
    public boolean isLive() {
        return this.liveAt != 0 && System.currentTimeMillis() - this.liveAt < 2000;
    }

    /**
     * The members below the "Coop (n)" header without {@code self}, or null if there is no such header or it doesn't
     * list {@code self} (then it is someone else's co-op).
     */
    static @Nullable List<Member> parseMembers(@NotNull List<String> lines, @NotNull String self) {
        for (int i = 0; i < lines.size(); i++) {
            final Matcher header = HEADER_PATTERN.matcher(lines.get(i));
            if (!header.matches()) {
                continue;
            }
            final int count = Integer.parseInt(header.group(1));
            final List<Member> members = new ArrayList<>();
            boolean listsSelf = false;
            for (int j = i + 1; j < lines.size() && j <= i + count; j++) {
                final Matcher member = MEMBER_PATTERN.matcher(lines.get(j));
                if (!member.matches()) {
                    break;
                }
                final String name = member.group(1);
                if (name.equalsIgnoreCase(self)) {
                    listsSelf = true;
                    continue;
                }
                final Matcher offline = OFFLINE_PATTERN.matcher(member.group(2));
                members.add(offline.find()
                        ? new Member(name, false, offline.group(1))
                        : new Member(name, true, null));
            }
            return listsSelf ? members : null;
        }
        return null;
    }

}
