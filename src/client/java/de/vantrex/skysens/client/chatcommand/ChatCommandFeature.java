package de.vantrex.skysens.client.chatcommand;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import net.minecraft.network.chat.Component;

/** Answers !commands sent in co-op and party chat, see {@link ChatCommandService}. */
@SkySensFeature
public class ChatCommandFeature implements GameMessageListeningFeature {

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        ChatCommandService.getInstance().onChatMessage(message.getString());
    }

    @Override
    public boolean isActive() {
        final var config = SkysensConfig.getInstance().chatCommandsCategory;
        return config.coopEnabled || config.partyEnabled;
    }

}
