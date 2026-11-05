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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
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
 * and the warped player. One warp runs at a time, further requests wait in a queue. All methods run on the client
 * thread.
 */
public final class PartyWarpService {

    /** Hypixel invites expire after 60 seconds. */
    public static final long INVITE_TIMEOUT_MS = 60_000;
    /** Pause between our party commands, Hypixel kicks for sending commands too fast. */
    public static final long COMMAND_DELAY_MS = 1000;
    /** Time the warp gets before the party is disbanded. */
    private static final long DISBAND_DELAY_MS = 3000;
    /** Pause between a disband and the next queued warp. */
    private static final long NEXT_WARP_DELAY_MS = 2000;
    private static final int MAX_QUEUE = 5;

    // Multi-line messages wrapped in dashes, so this is searched with find(). Anchored to a whole line,
    // so a player typing the same text in chat ("Co-op > X: ...") doesn't match.
    private static final Pattern INVITED_PATTERN = Pattern.compile("^(?:\\[[^]]+] )?(\\w{1,16}) has invited you to join their party! *$", Pattern.MULTILINE);

    @Getter
    private static final PartyWarpService instance = new PartyWarpService();

    // the requesting side: whose invite to accept, and until when
    private String acceptInviteFrom;
    private long acceptInviteUntil;

    // the warping side: the running warp (null when idle), whether its player joined, and who waits
    private @Nullable WarpRequest current;
    private boolean joined;
    /** Counts warps, so delayed tasks of an earlier warp do nothing. */
    private int warpId;
    /** Pause after a warp, so the next one doesn't start before the disband went through. */
    private boolean coolingDown;
    private final Deque<WarpRequest> queue = new ArrayDeque<>();

    private PartyWarpService() {
        EventListenerRegistry.REGISTRY.registerListener(event -> this.onPartyMemberJoin(event.getMember()),
                PartyMemberJoinEvent.class);
    }

    /** Invites {@code player} and warps them once they joined, see {@link #warp(String, Consumer, BooleanSupplier, String)}. */
    public void warp(@NotNull String player, @NotNull Consumer<String> reply) {
        this.warp(player, reply, () -> true, "");
    }

    /**
     * Invites {@code player} and warps them once they joined. While another warp runs, the request waits in a queue of
     * up to {@value #MAX_QUEUE} players and the player is told so.
     *
     * @param reply           sends a message to the player, e.g. when we are in a party and can't warp them
     * @param canWarp         checked again when a queued request starts, e.g. whether we are still on the right server
     * @param cannotWarpReply sent when {@code canWarp} is false by then
     */
    public void warp(@NotNull String player, @NotNull Consumer<String> reply, @NotNull BooleanSupplier canWarp,
                     @NotNull String cannotWarpReply) {
        if (this.isRequested(player)) {
            ClientUtil.sendDebug("Not warping " + player + " twice, already requested");
            return;
        }
        final WarpRequest request = new WarpRequest(player, reply, canWarp, cannotWarpReply);
        if (this.current == null && !this.coolingDown && this.queue.isEmpty()) {
            this.start(request);
            return;
        }
        if (this.queue.size() >= MAX_QUEUE) {
            reply.accept("Can't warp you right now, too many warps queued. Try again in a bit");
            return;
        }
        this.queue.addLast(request);
        reply.accept("Warping someone else right now, you're #" + this.queue.size() + " in line");
    }

    private boolean isRequested(String player) {
        return (this.current != null && this.current.player().equalsIgnoreCase(player))
                || this.queue.stream().anyMatch(request -> request.player().equalsIgnoreCase(player));
    }

    private void start(WarpRequest request) {
        if (!request.canWarp().getAsBoolean()) {
            request.reply().accept(request.cannotWarpReply());
            this.startNext();
            return;
        }
        // reserve now, so a second request can't start while we wait for the party info
        this.current = request;
        this.joined = false;
        final int id = ++this.warpId;
        final PartyTracker party = PartyTracker.getInstance();
        if (party.isInParty() && party.hasMember(request.player())) {
            request.reply().accept("You're already in my party, use /party warp");
            this.finish(id);
            return;
        }
        // inviting while in a party would add the player to it and the disband would end it, so always ask Hypixel:
        // right after a disband the cached state can still say we're in a party
        party.checkInParty().thenAccept(inParty -> Minecraft.getInstance().execute(() -> {
            if (id != this.warpId) {
                return;
            }
            if (inParty.isEmpty()) {
                request.reply().accept("Can't warp you, could not check my party status");
                this.finish(id);
            } else if (inParty.get()) {
                request.reply().accept("Can't warp you, I'm already in a party");
                this.finish(id);
            } else {
                sendCommand("party invite " + request.player());
                // not joined in time: the invite expired and Hypixel removed the empty party itself
                schedule(() -> {
                    if (id == this.warpId && !this.joined) {
                        ClientUtil.sendDebug("Warp: " + request.player() + " didn't accept the invite");
                        this.finish(id);
                    }
                }, INVITE_TIMEOUT_MS);
            }
        }));
    }

    /** Ends warp {@code id} and starts the next queued one after a pause, so the disband went through. */
    private void finish(int id) {
        if (id != this.warpId || this.current == null) {
            return;
        }
        this.current = null;
        this.joined = false;
        this.coolingDown = true;
        schedule(() -> {
            this.coolingDown = false;
            this.startNext();
        }, NEXT_WARP_DELAY_MS);
    }

    private void startNext() {
        final WarpRequest next = this.queue.pollFirst();
        if (next != null) {
            this.start(next);
        }
    }

    /**
     * Accepts the party invite from {@code inviter} for the next {@value #INVITE_TIMEOUT_MS} ms, if "Auto-accept warp
     * invite" is on.
     */
    public void acceptInviteFrom(@NotNull String inviter) {
        this.acceptInviteFrom(inviter, INVITE_TIMEOUT_MS);
    }

    /** Accepts the party invite from {@code inviter} for the next {@code durationMs}, e.g. longer while queued. */
    public void acceptInviteFrom(@NotNull String inviter, long durationMs) {
        if (!SkysensConfig.getInstance().chatCommandsCategory.autoAcceptWarpInvite) {
            return;
        }
        this.acceptInviteFrom = inviter;
        this.acceptInviteUntil = System.currentTimeMillis() + durationMs;
        ClientUtil.sendPrefixed("§7Accepting the party invite from §f" + inviter + " §7for the next " + durationMs / 1000 + " seconds.");
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
        if (this.current == null || this.joined || !member.equalsIgnoreCase(this.current.player())) {
            return;
        }
        this.joined = true;
        final int id = this.warpId;
        final String player = this.current.player();
        schedule(() -> sendCommand("party warp"), COMMAND_DELAY_MS);
        schedule(() -> {
            if (this.onlyPlayerInParty(player)) {
                sendCommand("party disband");
            } else {
                ClientUtil.sendPrefixed("§cNot disbanding the party, someone else is in it.");
            }
            this.finish(id);
        }, COMMAND_DELAY_MS + DISBAND_DELAY_MS);
    }

    /**
     * Whether the party only holds us and {@code player}. start() confirmed we weren't in a party before the invite, so
     * an unknown rank is fine; another member, or anyone else leading, means it isn't our party.
     */
    private boolean onlyPlayerInParty(String player) {
        final PartyTracker party = PartyTracker.getInstance();
        final PartyRole role = party.getRole();
        if ((role != null && role != PartyRole.LEADER) || party.isLedByOtherMember()) {
            return false;
        }
        return party.getMembers().stream().allMatch(member -> player.equalsIgnoreCase(member.getName()));
    }

    private record WarpRequest(String player, Consumer<String> reply, BooleanSupplier canWarp, String cannotWarpReply) {
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
