package de.vantrex.skysens.client.features.warp;

import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.skyblock.party.PartyWarpService;
import de.vantrex.skysens.client.skyblock.party.PartyTracker;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs {@code !warpto <player>}: player A types it, player B (the target) warps A to their server.
 * <ol>
 *     <li>A's client remembers B and auto-accepts B's party invite for {@value #INVITE_TIMEOUT_MS} ms.</li>
 *     <li>B's client checks that B is not in a party, then invites A.</li>
 *     <li>Once A joined, B runs /party warp and disbands the party again.</li>
 * </ol>
 * B's side (steps 2 and 3) is {@link PartyWarpService}, shared with {@code !warpme}. All methods run on the client
 * thread.
 */
public final class WarpToService {

    private static final long INVITE_TIMEOUT_MS = PartyWarpService.INVITE_TIMEOUT_MS;

    // Multi-line messages wrapped in dashes, so these are searched with find(). Anchored to a whole line,
    // so a player typing the same text in chat ("Co-op > X: ...") doesn't match.
    private static final Pattern INVITED_PATTERN = Pattern.compile("^(?:\\[[^]]+] )?(\\w{1,16}) has invited you to join their party! *$", Pattern.MULTILINE);
    private static final Pattern NAME_PATTERN = Pattern.compile("\\w{1,16}");

    @Getter
    private static final WarpToService instance = new WarpToService();

    // A's side: whose invite to accept, and until when
    private String acceptInviteFrom;
    private long acceptInviteUntil;

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
        // only a hint, so the cached state is good enough
        if (PartyTracker.getInstance().isInParty()) {
            ClientUtil.sendPrefixed("§cYou are in a party, leave it so you can accept the warp invite.");
        }
    }

    private void startWarp(ChatCommandContext context) {
        if (SkysensConfig.getInstance().chatCommandsCategory.commands.warpTo) {
            PartyWarpService.getInstance().warp(context.sender(), context::reply);
        }
    }

    /** Called for every chat message, formatting codes included. */
    public void onChatMessage(@NotNull String rawMessage) {
        final String message = ChatFormatting.stripFormatting(rawMessage);
        if (message == null) {
            return;
        }
        this.handleInvite(message);
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
            PartyWarpService.schedule(() -> PartyWarpService.sendCommand("party accept " + inviter), PartyWarpService.COMMAND_DELAY_MS);
        }
    }

    private static String ownName() {
        final var player = Minecraft.getInstance().player;
        return player == null ? null : player.getName().getString();
    }

}
