package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/** Fired by {@code DianaTracker} when {@code isDoingDiana()} flips (hub, ritual active, spade in the hotbar). Not fired when it first becomes known. */
@Data
public final class DianaDoingChangeEvent implements SkysensEvent {

    private final boolean doing;
}
