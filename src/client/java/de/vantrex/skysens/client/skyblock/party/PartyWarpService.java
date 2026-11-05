package de.vantrex.skysens.client.skyblock.party;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.skyblock.event.PartyMemberJoinEvent;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Warps a player to our server via a temporary party, used by {@code !warpto} and {@code !warpme}:
 * <ol>
 *     <li>The requesting player's client auto-accepts the warper's invite for {@value #INVITE_TIMEOUT_MS} ms, see
 *     {@link #acceptInviteFrom(String)}.</li>
 *     <li>The warper checks that they are not in a party, then invites the player, see {@link #warp}.</li>
 *     <li>Once the player joined, the warper runs /party warp and disbands the party again.</li>
 * </ol>
 * Party state comes from the {@link PartyTracker}. We never touch a party we did not create here: before inviting, a
 * cached "not in a party" is confirmed with the Hypixel Mod API, and the party is only disbanded while it holds just us
 * and the warped player. One warp runs at a time. All methods run on the client thread.
 */
public final class PartyWarpService {

    /** Hypixel invites expire after 60 seconds. */
    public static final long INVITE_TIMEOUT_MS = 60_000;
    /** Pause between our party commands, Hypixel kicks for sending commands too fast. */
    public static final long COMMAND_DELAY_MS = 1000;
    /** Time the warp gets before the party is disbanded. */
    private static final long DISBAND_DELAY_MS = 3000;

    // Multi-line messages wrapped in dashes, so this is searched with find(). Anchored to a whole line,
    // so a player typing the same text in chat ("Co-op > X: ...") doesn't match.
    private static final Pattern INVITED_PATTERN = Pattern.compile("^(?:\\[[^]]+] )?(\\w{1,16}) has invited you to join their party! *$", Pattern.MULTILINE);

    @Getter
    private static final PartyWarpService instance = new PartyWarpService();

    // the requesting side: whose invite to accept, and until when
    private String acceptInviteFrom;
    private long acceptInviteUntil;

    // who we are warping, null when idle
    private String warpingPlayer;
    private long warpStartedAt;
    private boolean joined;

    private PartyWarpService() {
        EventListenerRegistry.REGISTRY.registerListener(event -> this.onPartyMemberJoin(event.getMember()),
                PartyMemberJoinEvent.class);
    }

    /**
     * Invites {@code player} and warps them once they joined.
     *
     * @param reply sends a message to the player, e.g. when we are in a party and can't warp them
     */
    public void warp(@NotNull String player, @NotNull Consumer<String> reply) {
        if (this.isWarping()) {
            ClientUtil.sendDebug("Not warping " + player + ", already warping " + this.warpingPlayer);
            return;
        }
        // reserve now, so a second request can't start while we wait for the party info
        this.warpingPlayer = player;
        this.warpStartedAt = System.currentTimeMillis();
        this.joined = false;
        final PartyTracker party = PartyTracker.getInstance();
        if (party.isInParty()) {
            this.reset();
            reply.accept(party.hasMember(player)
                    ? "You're already in my party, use /party warp"
                    : "Can't warp you, I'm already in a party");
            return;
        }
        // inviting while in a party would add the player to it and the disband would end it, so ask Hypixel to be sure
        party.checkInParty().thenAccept(inParty -> {
            if (inParty.isEmpty()) {
                this.reset();
                reply.accept("Can't warp you, could not check my party status");
            } else if (inParty.get()) {
                this.reset();
                reply.accept("Can't warp you, I'm already in a party");
            } else {
                sendCommand("party invite " + player);
            }
        });
    }

    /**
     * Accepts the party invite from {@code inviter} for the next {@value #INVITE_TIMEOUT_MS} ms, if "Auto-accept warp
     * invite" is on.
     */
    public void acceptInviteFrom(@NotNull String inviter) {
        if (!SkysensConfig.getInstance().chatCommandsCategory.autoAcceptWarpInvite) {
            return;
        }
        this.acceptInviteFrom = inviter;
        this.acceptInviteUntil = System.currentTimeMillis() + INVITE_TIMEOUT_MS;
        ClientUtil.sendPrefixed("§7Accepting the party invite from §f" + inviter + " §7for the next 60 seconds.");
        // only a hint, so the cached state is good enough
        if (PartyTracker.getInstance().isInParty()) {
            ClientUtil.sendPrefixed("§cYou are in a party, leave it so you can accept the warp invite.");
        }
    }

    /** Called by {@link PartyTracker} for every chat message, formatting stripped. */
    void onChatMessage(@NotNull String message) {
        if (this.acceptInviteFrom == null) {
            return;
        }
        if (System.currentTimeMillis() > this.acceptInviteUntil) {
            this.acceptInviteFrom = null;
            return;
        }
        final Matcher matcher = INVITED_PATTERN.matcher(message);
        if (matcher.find() && matcher.group(1).equalsIgnoreCase(this.acceptInviteFrom)) {
            final String inviter = matcher.group(1);
            this.acceptInviteFrom = null;
            schedule(() -> sendCommand("party accept " + inviter), COMMAND_DELAY_MS);
        }
    }

    private void onPartyMemberJoin(@NotNull String member) {
        if (!this.isWarping() || this.joined || !member.equalsIgnoreCase(this.warpingPlayer)) {
            return;
        }
        this.joined = true;
        schedule(() -> sendCommand("party warp"), COMMAND_DELAY_MS);
        schedule(() -> {
            if (this.onlyWarpingPlayerInParty()) {
                sendCommand("party disband");
            } else {
                ClientUtil.sendPrefixed("§cNot disbanding the party, someone else is in it.");
            }
            this.reset();
        }, COMMAND_DELAY_MS + DISBAND_DELAY_MS);
    }

    /**
     * Whether the party only holds us and the player we are warping. warp() confirmed we weren't in a party before the
     * invite, so an unknown rank is fine; another member, or anyone else leading, means it isn't our party.
     */
    private boolean onlyWarpingPlayerInParty() {
        final PartyTracker party = PartyTracker.getInstance();
        final PartyRole role = party.getRole();
        if ((role != null && role != PartyRole.LEADER) || party.isLedByOtherMember()) {
            return false;
        }
        return party.getMembers().stream().allMatch(member -> this.warpingPlayer.equalsIgnoreCase(member.getName()));
    }

    private boolean isWarping() {
        if (this.warpingPlayer == null) {
            return false;
        }
        // not joined in time: the invite expired and Hypixel removed the empty party itself
        if (!this.joined && System.currentTimeMillis() - this.warpStartedAt > INVITE_TIMEOUT_MS) {
            this.reset();
            return false;
        }
        return true;
    }

    private void reset() {
        this.warpingPlayer = null;
        this.joined = false;
    }

    public static void schedule(Runnable task, long delayMs) {
        SkysensClient.SCHEDULER.schedule(() -> Minecraft.getInstance().execute(task), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void sendCommand(String command) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            ClientUtil.sendDebug("Warp: /" + command);
            connection.sendCommand(command);
        }
    }

}
