package de.vantrex.skysens.client.skyblock.time;

/**
 * The SkyBlock calendar, computed from the real time: Year 1 began on 2019-06-11 17:55 UTC (epoch 1_560_275_700_000 ms,
 * the value the community wiki and SkyHanni's SkyBlock time also use). A day lasts 20 real minutes (24 SkyBlock hours of
 * 50 s), a month 31 days (10 h 20 min) and a year 12 months = 372 days = 124 real hours, which matches {@link #YEAR_MS}.
 */
public final class SkyblockCalendar {

    private static final long EPOCH_MS = 1_560_275_700_000L;
    public static final int DAYS_PER_MONTH = 31;
    public static final int MONTHS_PER_YEAR = 12;
    public static final long DAY_MS = 20L * 60 * 1000;
    public static final long MONTH_MS = DAYS_PER_MONTH * DAY_MS;
    public static final long YEAR_MS = MONTHS_PER_YEAR * MONTH_MS;

    static {
        // 124 real hours; guards the constants against edits
        if (YEAR_MS != 124L * 60 * 60 * 1000) {
            throw new IllegalStateException("SkyBlock year length mismatch");
        }
    }

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

    /** The real time (ms since the Unix epoch) at which the given SkyBlock year begins. */
    public static long yearStartMs(int year) {
        return EPOCH_MS + (year - 1L) * YEAR_MS;
    }

    /** The SkyBlock date at the given real time. */
    public static SkyblockDate date(long timeMs) {
        final long sinceEpoch = timeMs - EPOCH_MS;
        final int year = (int) Math.floorDiv(sinceEpoch, YEAR_MS) + 1;
        final long intoYear = Math.floorMod(sinceEpoch, YEAR_MS);
        final int month = (int) (intoYear / MONTH_MS);
        final long intoMonth = intoYear % MONTH_MS;
        final int day = (int) (intoMonth / DAY_MS) + 1;
        final long intoDay = intoMonth % DAY_MS;
        final long hourMs = DAY_MS / 24;
        final int hour = (int) (intoDay / hourMs);
        final int minute = (int) ((intoDay % hourMs) * 60 / hourMs);
        return new SkyblockDate(year, SkyblockMonth.ofIndex(month), day, hour, Math.min(minute, 59));
    }

    public static SkyblockDate now() {
        return date(System.currentTimeMillis());
    }

    /** The real time at which the given day (1-31) of the month begins in the given year. */
    public static long dayStartMs(int year, SkyblockMonth month, int day) {
        return yearStartMs(year) + month.ordinal() * MONTH_MS + (day - 1L) * DAY_MS;
    }

    /** Whether the current SkyBlock year is a Year of the Witch. */
    public static boolean isYearOfTheWitch() {
        final int year = currentYear();
        return year >= WITCH_FIRST_YEAR && (year - WITCH_FIRST_YEAR) % WITCH_INTERVAL_YEARS == 0;
    }

}
