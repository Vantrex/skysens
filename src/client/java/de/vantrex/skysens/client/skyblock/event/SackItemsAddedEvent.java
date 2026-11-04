package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.sack.SackItem;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired by {@code SackTracker} once per sack message ("[Sacks] +179 items. (Last 12s.)") with every item that went
 * into a sack. Never empty.
 */
@Data
public final class SackItemsAddedEvent implements SkysensEvent {

    private final @NotNull List<SackItem> items;
    /**
     * The time the message sums up ("(Last 12s.)"), ending when it was received: the items went into a sack at some
     * point in that window, not necessarily just now.
     */
    private final long windowMs;

}
