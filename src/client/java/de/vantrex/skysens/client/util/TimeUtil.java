package de.vantrex.skysens.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    public static final long SECOND_MS = 1_000L;
    public static final long MINUTE_MS = 60 * SECOND_MS;
    public static final long HOUR_MS = 60 * MINUTE_MS;
    public static final long DAY_MS = 24 * HOUR_MS;
    public static final long MONTH_MS = 30 * DAY_MS;
    public static final long YEAR_MS = 365 * DAY_MS;

    private static final long[] UNIT_MS = {YEAR_MS, MONTH_MS, DAY_MS, HOUR_MS, MINUTE_MS};
    private static final String[] UNIT_NAMES = {"y", "mo", "d", "h", "m"};

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([hms])");

    private TimeUtil() {
    }

    /**
     * Long duration format: 1y 2mo 3d 4h 5m. Units that are 0 are left out, seconds are only shown below one hour
     * (12m 3s). A month counts as 30 days, a year as 365 days.
     */
    public static @NotNull String formatDuration(long millis) {
        long remaining = Math.max(0, millis);
        final boolean showSeconds = remaining < HOUR_MS;
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < UNIT_MS.length; i++) {
            final long value = remaining / UNIT_MS[i];
            remaining %= UNIT_MS[i];
            if (value > 0) {
                out.append(out.isEmpty() ? "" : " ").append(value).append(UNIT_NAMES[i]);
            }
        }
        final long seconds = remaining / SECOND_MS;
        if (showSeconds && (seconds > 0 || out.isEmpty())) {
            out.append(out.isEmpty() ? "" : " ").append(seconds).append("s");
        }
        return out.toString();
    }

    /** Compact minute format: &lt;1m, 45m, 2h, 2h5m. */
    public static @NotNull String formatHoursMinutes(long millis) {
        final long minutes = millis / MINUTE_MS;
        if (minutes < 1) {
            return "<1m";
        }
        if (minutes < 60) {
            return minutes + "m";
        }
        final long hours = minutes / 60;
        return minutes % 60 == 0 ? hours + "h" : hours + "h" + minutes % 60 + "m";
    }

    /** Rounded to whole seconds: 45s, 3m 5s. */
    public static @NotNull String formatMinutesSeconds(long millis) {
        final long seconds = Math.round(millis / 1000.0);
        return seconds >= 60 ? seconds / 60 + "m " + seconds % 60 + "s" : seconds + "s";
    }

    /** Run time format with padded seconds: 3m05s. */
    public static @NotNull String formatRunTime(double seconds) {
        final long total = Math.round(seconds);
        return (total / 60) + "m" + String.format(Locale.ROOT, "%02d", total % 60) + "s";
    }

    /** How long ago something happened: just now, 5 min ago, 3 h ago, 2 d ago. */
    public static @NotNull String formatAge(long minutes) {
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        final long hours = minutes / 60;
        return hours < 24 ? hours + " h ago" : hours / 24 + " d ago";
    }

    /** Parses "1h 02m 03s" style durations into milliseconds. */
    public static long parseDuration(@NotNull String text) {
        long millis = 0;
        final Matcher matcher = DURATION_PART.matcher(text);
        while (matcher.find()) {
            final long value = Long.parseLong(matcher.group(1));
            millis += switch (matcher.group(2)) {
                case "h" -> value * HOUR_MS;
                case "m" -> value * MINUTE_MS;
                default -> value * SECOND_MS;
            };
        }
        return millis;
    }

    /** Decodes kotlin.time.Duration's raw long: nanoseconds or milliseconds, marked by the lowest bit. */
    public static long kotlinDurationToMillis(long raw) {
        final long value = raw >> 1;
        return (raw & 1) == 0 ? value / 1_000_000 : value;
    }

}
