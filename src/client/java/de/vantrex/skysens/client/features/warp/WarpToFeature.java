package de.vantrex.skysens.client.features.warp;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.skyblock.event.PartyMemberJoinEvent;
import net.minecraft.network.chat.Component;

/** Feeds party invite messages and party joins to the !warpto flow, see {@link WarpToService}. */
@SkySensFeature
public class WarpToFeature implements GameMessageListeningFeature {

    public WarpToFeature() {
        EventListenerRegistry.REGISTRY.registerListener(event -> {
            if (this.isActive()) {
                WarpToService.getInstance().onPartyMemberJoin(event.getMember());
            }
        }, PartyMemberJoinEvent.class);
    }

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
