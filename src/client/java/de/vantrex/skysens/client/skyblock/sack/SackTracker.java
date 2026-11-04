package de.vantrex.skysens.client.skyblock.sack;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.skyblock.event.SackItemsAddedEvent;
import de.vantrex.skysens.client.skyblock.event.SackItemsRemovedEvent;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Turns Hypixel's sack messages into {@link SackItemsAddedEvent} and {@link SackItemsRemovedEvent}, see
 * {@link SackMessage}. Only on SkyBlock, and only if Hypixel's sack notifications are on. Client thread.
 */
public final class SackTracker implements GameMessageListeningFeature {

    @Getter
    private static final SackTracker instance = new SackTracker();

    private SackTracker() {
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !SkysensClient.getInstance().isOnSkyBlock() || !SackMessage.isSackMessage(message)) {
            return;
        }
        final long windowMs = SackMessage.windowMs(message);
        final List<SackItem> added = SackMessage.added(message);
        if (!added.isEmpty()) {
            EventListenerRegistry.REGISTRY.fireEvent(new SackItemsAddedEvent(List.copyOf(added), windowMs));
        }
        final List<SackItem> removed = SackMessage.removed(message);
        if (!removed.isEmpty()) {
            EventListenerRegistry.REGISTRY.fireEvent(new SackItemsRemovedEvent(List.copyOf(removed), windowMs));
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

}
