package de.vantrex.skysens.client.feature.dungeon;

import de.vantrex.skysens.client.repository.DungeonRunRepository;
import de.vantrex.skysens.client.repository.RepositoryRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Statistics of completed dungeon runs and the profit of their reward chests, kept per floor (e.g. "M7", "F5")
 * for this session (in memory), today and all time (both in {@link DungeonRunRepository}).
 */
public final class DungeonRunStats {

    private static final DungeonRunStats instance = new DungeonRunStats();

    public static DungeonRunStats getInstance() {
        return instance;
    }

    public enum Scope {
        SESSION("this session"),
        DAY("today"),
        ALL_TIME("yet");

        /** Ends "No M7 runs ..." */
        public final String noRunsSuffix;

        Scope(String noRunsSuffix) {
            this.noRunsSuffix = noRunsSuffix;
        }

        /** "day"/"today" or "alltime"/"all", null for anything else. */
        public static @Nullable Scope parse(@NotNull String arg) {
            return switch (arg.toLowerCase(Locale.ROOT)) {
                case "day", "today" -> DAY;
                case "alltime", "all" -> ALL_TIME;
                default -> null;
            };
        }
    }

    /** Totals of the runs of one floor. */
    public static final class FloorStats {
        public int runs;
        public int sPlusRuns;
        public int deaths;
        public long totalSeconds;
        public long bestSeconds = Long.MAX_VALUE;
        public long totalScore;
        /** Opened reward chests of this floor, in the run or at Croesus. */
        public int chests;
        /** Profit of {@link #chests}, rewards minus open cost. */
        public double chestProfit;

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

        public double chestProfitPerRun() {
            return this.chestProfit / this.runs;
        }

        /** Chest profit per hour spent in runs; downtime between runs isn't counted. */
        public double chestProfitPerHour() {
            return this.chestProfit / this.totalSeconds * 3600;
        }
    }

    /** Insertion ordered, a floor moves to the end when it gets a new run. */
    private final Map<String, FloorStats> session = new LinkedHashMap<>();
    /** The floor of the current (or last) dungeon instance, null outside of floors (e.g. Entrance). */
    private @Nullable String currentFloor;

    /** Records a finished run and returns the session stats of its floor, now including it. */
    public synchronized @NotNull FloorStats record(@NotNull String floor, long seconds, int score, boolean sPlus, int deaths) {
        FloorStats sessionStats = null;
        for (final Map<String, FloorStats> floors : this.allScopes()) {
            final FloorStats target = moveToEnd(floors, floor);
            target.runs++;
            target.totalSeconds += seconds;
            target.bestSeconds = Math.min(target.bestSeconds, seconds);
            target.totalScore += score;
            target.deaths += deaths;
            if (sPlus) {
                target.sPlusRuns++;
            }
            if (sessionStats == null) {
                sessionStats = target;
            }
        }
        return sessionStats;
    }

    /** Records the profit of an opened reward chest of the floor, without changing which floor was run last. */
    public synchronized void recordChest(@NotNull String floor, double profit) {
        for (final Map<String, FloorStats> floors : this.allScopes()) {
            final FloorStats target = floors.computeIfAbsent(floor, name -> new FloorStats());
            target.chests++;
            target.chestProfit += profit;
        }
    }

    public synchronized void setCurrentFloor(@Nullable String floor) {
        this.currentFloor = floor;
    }

    public synchronized @Nullable String getCurrentFloor() {
        return this.currentFloor;
    }

    public synchronized int getTotalRuns(@NotNull Scope scope) {
        return this.floors(scope).values().stream().mapToInt(stats -> stats.runs).sum();
    }

    public synchronized double getTotalChestProfit(@NotNull Scope scope) {
        return this.floors(scope).values().stream().mapToDouble(stats -> stats.chestProfit).sum();
    }

    /** The floor with the most recent run, or null if there is none. */
    public synchronized @Nullable String getLatestFloor(@NotNull Scope scope) {
        String latest = null;
        for (final Map.Entry<String, FloorStats> floor : this.floors(scope).entrySet()) {
            if (floor.getValue().runs > 0) {
                latest = floor.getKey();
            }
        }
        return latest;
    }

    public synchronized @Nullable FloorStats get(@NotNull Scope scope, @NotNull String floor) {
        return this.floors(scope).get(floor.toUpperCase(Locale.ROOT));
    }

    /** Finds the floor matching "m7", "f5" or just "7" (the most recently run one if both F7 and M7 match). */
    public synchronized @Nullable String find(@NotNull Scope scope, @NotNull String query) {
        final String wanted = query.toUpperCase(Locale.ROOT);
        String found = null;
        for (final String floor : this.floors(scope).keySet()) {
            if (floor.equals(wanted) || floor.substring(1).equals(wanted)) {
                found = floor;
            }
        }
        return found;
    }

    private Map<String, FloorStats> floors(Scope scope) {
        return switch (scope) {
            case SESSION -> this.session;
            case DAY -> this.dayFloors();
            case ALL_TIME -> repository().allTime;
        };
    }

    /** Session, today and all time, in that order. */
    private List<Map<String, FloorStats>> allScopes() {
        return List.of(this.session, this.dayFloors(), repository().allTime);
    }

    /** Today's floors, starting over when the stored ones belong to an earlier day. */
    private Map<String, FloorStats> dayFloors() {
        final DungeonRunRepository repository = repository();
        final String today = LocalDate.now().toString();
        if (!today.equals(repository.day) || repository.dayFloors == null) {
            repository.day = today;
            repository.dayFloors = new LinkedHashMap<>();
        }
        return repository.dayFloors;
    }

    private static FloorStats moveToEnd(Map<String, FloorStats> floors, String floor) {
        final FloorStats previous = floors.remove(floor);
        final FloorStats target = previous == null ? new FloorStats() : previous;
        floors.put(floor, target);
        return target;
    }

    private static DungeonRunRepository repository() {
        return RepositoryRegistry.DUNGEON_RUN_REPOSITORY;
    }

}
