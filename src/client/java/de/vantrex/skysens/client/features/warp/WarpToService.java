package de.vantrex.skysens.client.features.warp;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs {@code !warpto <player>}: player A types it, player B (the target) warps A to their server.
 * <ol>
 *     <li>A's client remembers B and auto-accepts B's party invite for {@value #INVITE_TIMEOUT_MS} ms.</li>
 *     <li>B's client checks that B is not in a party, then invites A.</li>
 *     <li>Once A joined, B runs /party warp and disbands the party again.</li>
 * </ol>
 * B never touches a party it did not create here. All methods run on the client thread.
 */
public final class WarpToService {

    /** Hypixel invites expire after 60 seconds. */
    private static final long INVITE_TIMEOUT_MS = 60_000;
    /** Pause between our party commands, Hypixel kicks for sending commands too fast. */
    private static final long COMMAND_DELAY_MS = 1000;
    /** Time the warp gets before the party is disbanded. */
    private static final long DISBAND_DELAY_MS = 3000;

    // Multi-line messages wrapped in dashes, so these are searched with find(). Anchored to a whole line,
    // so a player typing the same text in chat ("Co-op > X: ...") doesn't match.
    private static final Pattern INVITED_PATTERN = Pattern.compile("^(?:\\[[^]]+] )?(\\w{1,16}) has invited you to join their party! *$", Pattern.MULTILINE);
    private static final Pattern JOINED_PATTERN = Pattern.compile("^(?:\\[[^]]+] )?(\\w{1,16}) joined the party\\. *$", Pattern.MULTILINE);
    private static final Pattern NAME_PATTERN = Pattern.compile("\\w{1,16}");

    @Getter
    private static final WarpToService instance = new WarpToService();

    // A's side: whose invite to accept, and until when
    private String acceptInviteFrom;
    private long acceptInviteUntil;

    // B's side: who we are warping, 0 when idle
    private String warpingPlayer;
    private long warpStartedAt;
    private boolean joined;

    private WarpToService() {
    }

    /** Called for every {@code !warpto} message, on every client that sees it. */
    public void onCommand(@NotNull ChatCommandContext context) {
        if (!context.hasArgs()) {
            return;
        }
        final String target = context.arg(0);
        final String self = ownName();
        if (self == null || !NAME_PATTERN.matcher(target).matches() || target.equalsIgnoreCase(context.sender())) {
            return;
        }
        if (context.sender().equalsIgnoreCase(self)) {
            this.armAutoAccept(target);
        } else if (target.equalsIgnoreCase(self)) {
            this.startWarp(context);
        }
    }

    private void armAutoAccept(String inviter) {
        if (!SkysensConfig.getInstance().chatCommandsCategory.autoAcceptWarpInvite) {
            return;
        }
        this.acceptInviteFrom = inviter;
        this.acceptInviteUntil = System.currentTimeMillis() + INVITE_TIMEOUT_MS;
        ClientUtil.sendPrefixed("§7Accepting the party invite from §f" + inviter + " §7for the next 60 seconds.");
        PartyService.getInstance().isInParty().thenAccept(inParty -> {
            if (inParty.orElse(false)) {
                ClientUtil.sendPrefixed("§cYou are in a party, leave it so you can accept the warp invite.");
            }
        });
    }

    private void startWarp(ChatCommandContext context) {
        if (!SkysensConfig.getInstance().chatCommandsCategory.commands.warpTo) {
            return;
        }
        if (this.isWarping()) {
            ClientUtil.sendDebug("Ignoring !warpto from " + context.sender() + ", already warping " + this.warpingPlayer);
            return;
        }
        // reserve now, so a second request can't start while we wait for the party info
        this.warpingPlayer = context.sender();
        this.warpStartedAt = System.currentTimeMillis();
        this.joined = false;
        PartyService.getInstance().isInParty().thenAccept(inParty -> {
            if (inParty.isEmpty()) {
                this.reset();
                context.reply("Can't warp you, could not check my party status");
            } else if (inParty.get()) {
                this.reset();
                context.reply("Can't warp you, I'm already in a party");
            } else {
                sendCommand("party invite " + context.sender());
            }
        });
    }

    /** Called for every chat message, formatting codes included. */
    public void onChatMessage(@NotNull String rawMessage) {
        final String message = ChatFormatting.stripFormatting(rawMessage);
        if (message == null) {
            return;
        }
        this.handleInvite(message);
        this.handleJoin(message);
    }

    private void handleInvite(String message) {
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

    private void handleJoin(String message) {
        if (!this.isWarping() || this.joined) {
            return;
        }
        final Matcher matcher = JOINED_PATTERN.matcher(message);
        if (!matcher.find() || !matcher.group(1).equalsIgnoreCase(this.warpingPlayer)) {
            return;
        }
        this.joined = true;
        schedule(() -> sendCommand("party warp"), COMMAND_DELAY_MS);
        schedule(() -> {
            sendCommand("party disband");
            this.reset();
        }, COMMAND_DELAY_MS + DISBAND_DELAY_MS);
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

    private static String ownName() {
        final var player = Minecraft.getInstance().player;
        return player == null ? null : player.getName().getString();
    }

    private static void schedule(Runnable task, long delayMs) {
        SkysensClient.SCHEDULER.schedule(() -> Minecraft.getInstance().execute(task), delayMs, TimeUnit.MILLISECONDS);
    }

    private static void sendCommand(String command) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            ClientUtil.sendDebug("Warp: /" + command);
            connection.sendCommand(command);
        }
    }

}
