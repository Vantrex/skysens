package de.vantrex.skysens.client.handler;

import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.service.FeatureService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

public class ChatHandler {

    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            // our own messages (debug, summaries) can be sent while a feature handles a message
            if (ClientUtil.isSendingOwnMessage()) {
                return;
            }
            FEATURE_REGISTRY.getGameMessageListeningFeatures()
                    .forEach(feature -> feature.onGameMessage(message, overlay));
        });
    }

}
