package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.skyblock.time.SkyblockCalendar;
import de.vantrex.skysens.client.skyblock.time.SkyblockCalendarEvent;
import de.vantrex.skysens.client.skyblock.time.SkyblockDate;
import de.vantrex.skysens.client.skyblock.time.SkyblockEventCalendar;

/** The SkyBlock clock and calendar events. Pure math on the real time, never unknown; read once per press. */
public final class TimeFacts {

    private SkyblockDate date;

    public SkyblockDate date() {
        if (this.date == null) {
            this.date = SkyblockCalendar.now();
        }
        return this.date;
    }

    public boolean eventActive(SkyblockCalendarEvent event) {
        return SkyblockEventCalendar.isEventActive(event);
    }

    /** "14:05" */
    public String timeText() {
        final SkyblockDate d = this.date();
        return String.format("%02d:%02d", d.hour(), d.minute());
    }

    /** "Late Autumn 29, Year 450" */
    public String dateText() {
        final SkyblockDate d = this.date();
        return d.month().getDisplayName() + " " + d.day() + ", Year " + d.year();
    }

}
