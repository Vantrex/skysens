package de.vantrex.skysens.client.feature.chat;

import de.vantrex.skysens.client.chatcommand.warp.WarpToService;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import net.minecraft.network.chat.Component;

/** Feeds party invite and join messages to the !warpto flow, see {@link WarpToService}. */
@SkySensFeature
public class WarpToFeature implements GameMessageListeningFeature {

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive()) {
            return;
        }
        WarpToService.getInstance().onChatMessage(message.getString());
    }

    @Override
    public boolean isActive() {
        final var config = SkysensConfig.getInstance().chatCommandsCategory;
        return config.commands.warpTo || config.autoAcceptWarpInvite;
    }

}
