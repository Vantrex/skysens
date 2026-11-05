package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/** Fired by {@code DianaTracker} when the Mythological Ritual becomes active or inactive. Not fired when it first becomes known. */
@Data
public final class DianaRitualChangeEvent implements SkysensEvent {

    private final boolean active;
}
