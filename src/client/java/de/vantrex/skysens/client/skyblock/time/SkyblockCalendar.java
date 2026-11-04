package de.vantrex.skysens.client.skyblock.time;

/**
 * The SkyBlock calendar, computed from the real time: Year 1 began on 2019-06-11 17:55 UTC and a SkyBlock year
 * lasts 124 real hours (12 months of 31 days of 20 minutes).
 */
public final class SkyblockCalendar {

    private static final long EPOCH_MS = 1_560_275_700_000L;
    private static final long YEAR_MS = 124L * 60 * 60 * 1000;

    /** The Year of the Witch was first held in Year 476 and repeats every 12 years, lasting one whole year. */
    private static final int WITCH_FIRST_YEAR = 476;
    private static final int WITCH_INTERVAL_YEARS = 12;

    private SkyblockCalendar() {
    }

    /** The SkyBlock year at the given real time (ms since the Unix epoch). */
    public static int year(long timeMs) {
        return (int) Math.floorDiv(timeMs - EPOCH_MS, YEAR_MS) + 1;
    }

    public static int currentYear() {
        return year(System.currentTimeMillis());
    }

    /** Whether the current SkyBlock year is a Year of the Witch. */
    public static boolean isYearOfTheWitch() {
        final int year = currentYear();
        return year >= WITCH_FIRST_YEAR && (year - WITCH_FIRST_YEAR) % WITCH_INTERVAL_YEARS == 0;
    }

}
