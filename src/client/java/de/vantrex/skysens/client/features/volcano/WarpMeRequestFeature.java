package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.util.ChatUtil;
import net.minecraft.network.chat.Component;

/** Feeds chat messages to the requesting side of {@code !warpme}, see {@link WarpMeRequests}. */
@SkySensFeature
public class WarpMeRequestFeature implements GameMessageListeningFeature {

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        WarpMeRequests.onChatMessage(ChatUtil.strip(message.getString()));
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.autoAcceptWarpInvite;
    }

}
