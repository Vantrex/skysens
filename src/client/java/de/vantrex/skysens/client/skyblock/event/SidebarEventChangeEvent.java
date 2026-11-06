package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.sidebar.SidebarEventType;
import lombok.Data;

/** Fired by {@code SidebarEventTracker} when a timed sidebar event appears or disappears. Not fired for the first reading after joining. */
@Data
public final class SidebarEventChangeEvent implements SkysensEvent {

    private final SidebarEventType type;
    private final boolean active;

}
