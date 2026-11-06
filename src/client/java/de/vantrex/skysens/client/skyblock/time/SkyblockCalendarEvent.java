package de.vantrex.skysens.client.skyblock.time;

import lombok.Getter;

import java.util.List;

/**
 * Recurring SkyBlock events with a fixed, deterministic calendar window (inclusive day ranges). Sourced from the
 * SkyBlock wiki event pages.
 * <p>
 * Deliberately <b>unsupported</b> because the timing could not be confirmed here: Dark Auction (believed hourly at
 * :55 real time), Jacob's Contest (every 3 SkyBlock days, offset unconfirmed; use the sidebar tracker),
 * Hoppity's Hunt, Carnival, Fishing Festival, Mining Fiesta, election dates.
 */
@Getter
public enum SkyblockCalendarEvent {
    SPOOKY_FESTIVAL("Spooky Festival", new Window(SkyblockMonth.LATE_AUTUMN, 29, 31)),
    SEASON_OF_JERRY("Season of Jerry", new Window(SkyblockMonth.LATE_WINTER, 24, 26)),
    NEW_YEAR("New Year Celebration", new Window(SkyblockMonth.LATE_WINTER, 29, 31)),
    TRAVELING_ZOO("Traveling Zoo", new Window(SkyblockMonth.EARLY_SUMMER, 1, 3), new Window(SkyblockMonth.EARLY_WINTER, 1, 3));

    /** Days {@code firstDay} to {@code lastDay} (both inclusive) of a month, every year. */
    public record Window(SkyblockMonth month, int firstDay, int lastDay) {
    }

    private final String displayName;
    private final List<Window> windows;

    SkyblockCalendarEvent(String displayName, Window... windows) {
        this.displayName = displayName;
        this.windows = List.of(windows);
    }
}
