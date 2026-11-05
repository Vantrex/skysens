package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import org.jetbrains.annotations.Nullable;
import lombok.Data;

/** Fired by {@code DianaTracker} for a burrow treasure drop; {@code coins} is set when the drop was coins, {@code itemOrCoinsText} is the colourless text of the drop (e.g. "Griffin Feather" or "12,345 coins"). */
@Data
public final class DianaTreasureEvent implements SkysensEvent {

    private final String itemOrCoinsText;
    private final @Nullable Long coins;
}
