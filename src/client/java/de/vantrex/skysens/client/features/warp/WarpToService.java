package de.vantrex.skysens.client.features.warp;

import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.skyblock.party.PartyWarpService;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Runs {@code !warpto <player>}: player A types it, player B (the target) warps A to their server.
 * <ol>
 *     <li>A's client remembers B and auto-accepts B's party invite for 60 seconds.</li>
 *     <li>B's client checks that B is not in a party, then invites A.</li>
 *     <li>Once A joined, B runs /party warp and disbands the party again.</li>
 * </ol>
 * All three steps are {@link PartyWarpService}, shared with {@code !warpme}; this only parses the command. All methods
 * run on the client thread.
 */
public final class WarpToService {

    private static final Pattern NAME_PATTERN = Pattern.compile("\\w{1,16}");

    @Getter
    private static final WarpToService instance = new WarpToService();

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
            PartyWarpService.getInstance().acceptInviteFrom(target);
        } else if (target.equalsIgnoreCase(self)) {
            this.startWarp(context);
        }
    }

    private void startWarp(ChatCommandContext context) {
        if (SkysensConfig.getInstance().chatCommandsCategory.commands.warpTo) {
            PartyWarpService.getInstance().warp(context.sender(), context::reply);
        }
    }

    private static String ownName() {
        final var player = Minecraft.getInstance().player;
        return player == null ? null : player.getName().getString();
    }

}
