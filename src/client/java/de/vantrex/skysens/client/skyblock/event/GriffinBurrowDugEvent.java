package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/** Fired by {@code DianaTracker} for each burrow chat line "You dug out a Griffin Burrow! (x/y)"; {@code chainFinished} is true for "You finished the Griffin burrow chain!". */
@Data
public final class GriffinBurrowDugEvent implements SkysensEvent {

    private final int current;
    private final int max;
    private final boolean chainFinished;
}
