package de.vantrex.skysens.client.platform.hook;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.features.warp.WarpToMenuScreen;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.FeatureRegistry;
import de.vantrex.skysens.client.core.feature.FeatureService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class ChatHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");
    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();
    /** Just "!warpto" without a player, typed in a chat channel or with /cchat. */
    private static final Pattern BARE_WARPTO_CHAT = Pattern.compile("^\\s*!warpto\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_WARPTO_COMMAND = Pattern.compile("^(?:cc|cchat|coopchat)\\s+!warpto\\s*$", Pattern.CASE_INSENSITIVE);

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

    /**
     * A game message as the server sent it, from {@code SystemChatPacketMixin}, before Fabric's events and other mods.
     * A failing feature is logged and skipped: this runs inside packet handling, where an exception would disconnect.
     */
    public static void onRawGameMessage(Component message, boolean overlay) {
        for (final var feature : FEATURE_REGISTRY.getRawGameMessageListeningFeatures()) {
            try {
                feature.onRawGameMessage(message, overlay);
            } catch (RuntimeException e) {
                LOGGER.error("{} failed on a raw game message", feature.getClass().getSimpleName(), e);
            }
        }
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
