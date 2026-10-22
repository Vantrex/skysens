package de.vantrex.skysens.client.features.warp;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/** {@code !warpto <player>} warps the sender to the given co-op member, see {@link WarpToService}. */
public class WarpToCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "warpto";
    }

    @Override
    public @NotNull String description() {
        return "Warps you to that co-op member via a temporary party";
    }

    @Override
    public @NotNull String usage() {
        return "<player>";
    }

    @Override
    public boolean isEnabled() {
        // also needed on the sender's side, to auto-accept the invite
        final var config = SkysensConfig.getInstance().chatCommandsCategory;
        return config.commands.warpTo || config.autoAcceptWarpInvite;
    }

    @Override
    public @NotNull Set<ChatChannel> channels() {
        // in a party you can just use /party warp
        return EnumSet.of(ChatChannel.COOP);
    }

    @Override
    public boolean usesCooldown() {
        // every client sees the command but at most one replies; WarpToService runs one warp at a time
        return false;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        WarpToService.getInstance().onCommand(context);
    }

}
