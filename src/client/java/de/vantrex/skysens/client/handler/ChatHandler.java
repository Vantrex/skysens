package de.vantrex.skysens.client.handler;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.warp.WarpToMenuScreen;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.service.FeatureService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.regex.Pattern;

public class ChatHandler {

    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();
    /** Just "!warpto" without a player, typed in a chat channel or with /cc. */
    private static final Pattern BARE_WARPTO_CHAT = Pattern.compile("^\\s*!warpto\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_WARPTO_COMMAND = Pattern.compile("^(?:cc|coopchat)\\s+!warpto\\s*$", Pattern.CASE_INSENSITIVE);

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            // our own messages (debug, summaries) can be sent while a feature handles a message
            if (ClientUtil.isSendingOwnMessage()) {
                return;
            }
            FEATURE_REGISTRY.getGameMessageListeningFeatures()
                    .forEach(feature -> feature.onGameMessage(message, overlay));
        });
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !openWarpToMenu(BARE_WARPTO_CHAT, message));
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> !openWarpToMenu(BARE_WARPTO_COMMAND, command));
    }

    /** Opens the warp menu instead of sending a bare !warpto. Returns whether it did. */
    private static boolean openWarpToMenu(Pattern pattern, String message) {
        if (!SkysensClient.getInstance().isOnSkyBlock()
                || !SkysensConfig.getInstance().chatCommandsCategory.warpToMenu
                || !pattern.matcher(message).matches()) {
            return false;
        }
        WarpToMenuScreen.open();
        return true;
    }

}
