package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.time.SkyblockCalendarEvent;
import lombok.Data;

/** Fired when a calendar event (see {@code SkyblockEventCalendar}) starts or ends while on SkyBlock. */
@Data
public final class SkyblockCalendarEventChangeEvent implements SkysensEvent {

    private final SkyblockCalendarEvent event;
    private final boolean active;

}
