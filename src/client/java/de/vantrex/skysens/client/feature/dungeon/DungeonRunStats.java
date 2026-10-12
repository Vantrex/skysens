package de.vantrex.skysens.client.feature.dungeon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory statistics of the dungeon runs completed this session, kept per floor (e.g. "M7", "F5").
 * Not persisted; reset when the game restarts.
 */
public final class DungeonRunStats {

    private static final DungeonRunStats instance = new DungeonRunStats();

    public static DungeonRunStats getInstance() {
        return instance;
    }

    /** Totals of the runs of one floor. */
    public static final class FloorStats {
        public int runs;
        public int sPlusRuns;
        public int deaths;
        public long totalSeconds;
        public long bestSeconds = Long.MAX_VALUE;
        public long totalScore;

        public double avgSeconds() {
            return (double) this.totalSeconds / this.runs;
        }

        public double avgScore() {
            return (double) this.totalScore / this.runs;
        }

        public double sPlusRate() {
            return (double) this.sPlusRuns / this.runs;
        }

        public double deathsPerRun() {
            return (double) this.deaths / this.runs;
        }
    }

    /** Insertion ordered, a floor moves to the end when it gets a new run. */
    private final Map<String, FloorStats> floors = new LinkedHashMap<>();
    private int totalRuns;

    /** Records a finished run and returns the stats of its floor, now including it. */
    public synchronized @NotNull FloorStats record(@NotNull String floor, long seconds, int score, boolean sPlus, int deaths) {
        final FloorStats previous = this.floors.remove(floor);
        final FloorStats target = previous == null ? new FloorStats() : previous;
        this.floors.put(floor, target);
        target.runs++;
        target.totalSeconds += seconds;
        target.bestSeconds = Math.min(target.bestSeconds, seconds);
        target.totalScore += score;
        target.deaths += deaths;
        if (sPlus) {
            target.sPlusRuns++;
        }
        this.totalRuns++;
        return target;
    }

    public synchronized int getTotalRuns() {
        return this.totalRuns;
    }

    /** The floor with the most recent run, or null if there is none. */
    public synchronized @Nullable String getLatestFloor() {
        String latest = null;
        for (final String floor : this.floors.keySet()) {
            latest = floor;
        }
        return latest;
    }

    public synchronized @Nullable FloorStats get(@NotNull String floor) {
        return this.floors.get(floor.toUpperCase(Locale.ROOT));
    }

    /** Finds the most recent floor matching "m7", "f5" or just "7". */
    public synchronized @Nullable String find(@NotNull String query) {
        final String wanted = query.toUpperCase(Locale.ROOT);
        String found = null;
        for (final String floor : this.floors.keySet()) {
            if (floor.equals(wanted) || floor.substring(1).equals(wanted)) {
                found = floor;
            }
        }
        return found;
    }

    public static @NotNull String formatTime(double seconds) {
        final long total = Math.round(seconds);
        return (total / 60) + "m" + String.format(Locale.ROOT, "%02d", total % 60) + "s";
    }

}
