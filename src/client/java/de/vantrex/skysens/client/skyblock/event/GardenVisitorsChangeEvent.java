package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/** Fired when the Garden visitor count shown in the sidebar changes; both are -1 when the line disappears. */
@Data
public final class GardenVisitorsChangeEvent implements SkysensEvent {

    private final int current;
    private final int max;

}
