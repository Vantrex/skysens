package de.vantrex.skysens.client.feature.slayer;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.slayer.SlayerService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Feeds slayer related chat lines to {@link SlayerService}. */
@SkySensFeature
public class SlayerTrackingFeature implements GameMessageListeningFeature {

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text != null) {
            SlayerService.getInstance().onChatLine(text);
        }
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().slayerCategory.enabled;
    }

}
