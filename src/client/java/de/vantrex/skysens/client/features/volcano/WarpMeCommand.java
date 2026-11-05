package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.party.PartyWarpService;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * {@code !warpme} warps the sender to our Crimson Isle server via a temporary party, see {@link PartyWarpService}.
 * Only active while we are on the server of a CATACLYSMIC eruption we got a title for, or, on the requesting side,
 * shortly after we saw an announcement of one. Otherwise the command is off, so in party or co-op chat (where every
 * SkySens client sees it) only the client with the eruption acts.
 */
public class WarpMeCommand implements ChatCommand {

    private static final String ERUPTION_OVER = "Can't warp you anymore, the cataclysmic eruption is over or I left its server";

    @Override
    public @NotNull String name() {
        return "warpme";
    }

    @Override
    public @NotNull String description() {
        return "Warps you to my cataclysmic volcano eruption via a temporary party";
    }

    @Override
    public boolean isEnabled() {
        return isWarperActive() || isRequesterActive();
    }

    /** We have a cataclysmic eruption to warp players to. */
    private static boolean isWarperActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.warpMe
                && LocationService.getInstance().getCurrentLocation() == SkyblockLocationEnum.CRIMSON_ISLE
                && VolcanoFoundEruption.isCataclysmic();
    }

    /** We saw a cataclysmic eruption announcement and may want to auto-accept the warp invite. */
    private static boolean isRequesterActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.autoAcceptWarpInvite && WarpMeRequests.hasRecentAnnouncement();
    }

    @Override
    public @NotNull Set<ChatChannel> channels() {
        return EnumSet.allOf(ChatChannel.class);
    }

    @Override
    public boolean usesCooldown() {
        // only the client with the eruption replies, and PartyWarpService queues the warps
        return false;
    }

    @Override
    public boolean defersToOtherMods() {
        return false;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (player.getName().getString().equalsIgnoreCase(context.sender())) {
            // our own request in party or co-op chat; private ones are handled by WarpMeRequests
            if (isRequesterActive()) {
                WarpMeRequests.onOwnChannelRequest();
            }
            return;
        }
        if (isWarperActive()) {
            PartyWarpService.getInstance().warp(context.sender(), context::reply, WarpMeCommand::isWarperActive, ERUPTION_OVER);
        }
    }

}
