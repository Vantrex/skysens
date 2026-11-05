package de.vantrex.skysens.client.skyblock.time;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.skyblock.event.SkyblockCalendarEventChangeEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockQuitEvent;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tells which calendar events are running (pure math on the real time, never unknown) and fires
 * {@link SkyblockCalendarEventChangeEvent} when one starts or ends while on SkyBlock. The first check after joining
 * only records the state, so nothing fires for an event that was already running.
 */
public final class SkyblockEventCalendar implements AfterTickFeature {

    @Getter
    private static final SkyblockEventCalendar instance = new SkyblockEventCalendar();

    private static final int CHECK_INTERVAL_TICKS = 20;

    private final Map<SkyblockCalendarEvent, Boolean> lastState = new EnumMap<>(SkyblockCalendarEvent.class);
    private int ticks;

    private SkyblockEventCalendar() {
        EventListenerRegistry.REGISTRY.registerListener(event -> this.lastState.clear(), SkyblockQuitEvent.class);
    }

    public static boolean isEventActive(SkyblockCalendarEvent event) {
        return isEventActive(event, System.currentTimeMillis());
    }

    public static boolean isEventActive(SkyblockCalendarEvent event, long timeMs) {
        final SkyblockDate date = SkyblockCalendar.date(timeMs);
        for (final SkyblockCalendarEvent.Window window : event.getWindows()) {
            if (window.month() == date.month() && date.day() >= window.firstDay() && date.day() <= window.lastDay()) {
                return true;
            }
        }
        return false;
    }

    /** The real time (ms) of the next start strictly after now. */
    public static long nextStart(SkyblockCalendarEvent event) {
        return nextStart(event, System.currentTimeMillis());
    }

    public static long nextStart(SkyblockCalendarEvent event, long timeMs) {
        final int year = SkyblockCalendar.year(timeMs);
        long best = Long.MAX_VALUE;
        for (int y = year; y <= year + 1; y++) {
            for (final SkyblockCalendarEvent.Window window : event.getWindows()) {
                final long start = SkyblockCalendar.dayStartMs(y, window.month(), window.firstDay());
                if (start > timeMs && start < best) {
                    best = start;
                }
            }
        }
        return best;
    }

    @Override
    public void afterTick() {
        if (++this.ticks % CHECK_INTERVAL_TICKS != 0 || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final long now = System.currentTimeMillis();
        for (final SkyblockCalendarEvent event : SkyblockCalendarEvent.values()) {
            final boolean active = isEventActive(event, now);
            final Boolean previous = this.lastState.put(event, active);
            if (previous != null && previous != active) {
                EventListenerRegistry.REGISTRY.fireEvent(new SkyblockCalendarEventChangeEvent(event, active));
            }
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
