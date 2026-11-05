package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.party.PartyWarpService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * {@code !warpme} warps the sender to our Crimson Isle server via a temporary party, see {@link PartyWarpService}.
 * Only works while we are still on the server of an eruption we got a title for; otherwise it stays silent, because
 * in party or co-op chat every SkySens client sees the command and only the one with the eruption should act.
 */
public class WarpMeCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "warpme";
    }

    @Override
    public @NotNull String description() {
        return "Warps you to my volcano eruption via a temporary party";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.warpMe;
    }

    @Override
    public @NotNull Set<ChatChannel> channels() {
        return EnumSet.allOf(ChatChannel.class);
    }

    @Override
    public boolean usesCooldown() {
        // only the client with the eruption replies, and PartyWarpService runs one warp at a time
        return false;
    }

    @Override
    public boolean defersToOtherMods() {
        return false;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final var player = Minecraft.getInstance().player;
        if (player == null || player.getName().getString().equalsIgnoreCase(context.sender())) {
            return;
        }
        if (LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.CRIMSON_ISLE
                || VolcanoFoundEruption.current().isEmpty()) {
            ClientUtil.sendDebug("Ignoring !warpme from " + context.sender() + ", no eruption found on this server");
            return;
        }
        PartyWarpService.getInstance().warp(context.sender(), context::reply);
    }

}
