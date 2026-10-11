package de.vantrex.skysens.client.feature.chat;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatCommandService;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import net.minecraft.network.chat.Component;

/** Answers !commands sent in co-op chat, see {@link ChatCommandService}. */
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
        return SkysensConfig.getInstance().chatCommandsCategory.coopEnabled;
    }

}
