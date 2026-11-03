package de.vantrex.skysens.client.skyblock.profit;

import de.vantrex.skysens.client.integration.skyhanni.SkyHanniTrackerReader;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.util.ScoreboardUtil;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Profit tracking for {@code !profit}.
 * <ul>
 *     <li>Overall: with SkyHanni installed and selected as the tracker, the sum of SkyHanni's trackers and SkySens' reward chest trackers,
 *     otherwise SkySens' own tracker of everything it values (rare drops and reward chests). Its uptime is
 *     the time spent on SkyBlock, except for the all-time sum of SkyHanni's trackers, which uses the
 *     trackers' summed active time (see {@link OverallProfit}).</li>
 *     <li>Activities: the tracker that gained most recently. SkyHanni's trackers (Slayer, Fishing, ...) when
 *     SkyHanni is installed, SkySens' own per-activity trackers otherwise. Reward chests are always tracked
 *     by SkySens, SkyHanni has no tracker for them.</li>
 * </ul>
 * Session values live in memory, all-time values in {@link ProfitRepository}. Called on the client thread.
 */
public final class ProfitService {

    public static final String DUNGEONS = "Dungeons";
    public static final String KUUDRA = "Kuudra";

    /** A tick gap longer than this is a lag spike or a paused game, not time played. */
    private static final long MAX_TICK_GAP_MS = 1000;

    /** Stands in for SkyHanni's profile when SkySens' own tracker is used. */
    private static final Object OWN_PROFILE = new Object();

    @Getter
    private static final ProfitService instance = new ProfitService();

    private final SkyHanniTrackerReader skyHanni = new SkyHanniTrackerReader();
    private final ProfitStats sessionOverall = new ProfitStats();
    private final Map<String, ProfitStats> sessionActivities = new HashMap<>();
    private long lastTickMs;
    /** The session overall when the current day started (empty at launch), today's part is everything after it. */
    private ProfitStats dayBaseline = new ProfitStats();
    private LocalDate currentDay = LocalDate.now();
    private long lastDayCheckMs;
    private long lastSampleMs;
    /** The last session overall that was read while on SkyBlock, its profile is {@link #snapshotProfile}. */
    private ProfitStats lastSnapshot = new ProfitStats();
    private Object snapshotProfile;

    /** Profit of one tracker, for a session or all time. */
    public record ActivityProfit(@NotNull String name, @NotNull ProfitStats stats, @NotNull String source) {
    }

    /**
     * Overall profit. {@code timeOnSkyBlock} tells what its uptime is: the time spent on SkyBlock, or (all-time
     * SkyHanni totals) the trackers' summed active time.
     */
    public record OverallProfit(@NotNull ProfitStats stats, boolean timeOnSkyBlock) {
    }

    private ProfitService() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            this.countUptime();
            this.checkDayChange();
        });
        // runs before the shutdown hook that saves the repositories
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> this.foldDay());
    }

    private void countUptime() {
        final long now = System.currentTimeMillis();
        final long gap = now - this.lastTickMs;
        this.lastTickMs = now;
        if (!isEnabled() || !SkysensClient.getInstance().isOnSkyBlock() || gap <= 0 || gap > MAX_TICK_GAP_MS) {
            return;
        }
        this.sessionOverall.uptimeMs += gap;
        repository().overall.uptimeMs += gap;
    }

    /** At local midnight, stores what was made on the old day and starts counting the new one. */
    private void checkDayChange() {
        final long now = System.currentTimeMillis();
        if (now - this.lastDayCheckMs < 1000) {
            return;
        }
        this.lastDayCheckMs = now;
        // reading SkyHanni is heavy, so only sample once a minute
        if (now - this.lastSampleMs >= 60_000) {
            this.lastSampleMs = now;
            this.sampleDay();
        }
        final LocalDate today = LocalDate.now();
        if (today.equals(this.currentDay)) {
            return;
        }
        this.foldDay();
        this.currentDay = today;
        repository().day = today.toString();
        repository().dayStats = new ProfitStats();
    }

    /**
     * Takes a snapshot of the session overall, but only where it is comparable to the baseline: on SkyBlock, and
     * with SkyHanni on a known profile. SkyHanni's sessions are per profile, so on a profile change what the old
     * profile made is stored and the baseline starts over.
     */
    private synchronized void sampleDay() {
        if (!isEnabled() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final boolean useSkyHanni = this.useSkyHanni();
        final Object profile = useSkyHanni ? this.skyHanni.currentProfile() : OWN_PROFILE;
        if (profile == null) {
            return;
        }
        final ProfitStats current = copyOf(this.getOverall(false).stats());
        if (this.snapshotProfile != null && profile != this.snapshotProfile) {
            this.storeDayPart();
            this.dayBaseline = current;
        }
        this.lastSnapshot = current;
        this.snapshotProfile = profile;
    }

    /** Adds today's part (last snapshot minus baseline) to the repository, replacing it if it holds an older day. */
    private void storeDayPart() {
        final String day = this.currentDay.toString();
        if (!day.equals(repository().day) || repository().dayStats == null) {
            repository().day = day;
            repository().dayStats = new ProfitStats();
        }
        addTo(repository().dayStats, diff(this.lastSnapshot, this.dayBaseline));
        this.dayBaseline = this.lastSnapshot;
    }

    /** Stores today's part and restarts the baseline, so nothing is counted twice. */
    private synchronized void foldDay() {
        this.sampleDay();
        this.storeDayPart();
    }

    /** Overall profit made today, across game sessions. Its uptime is the time on SkyBlock. */
    public @NotNull OverallProfit getDay() {
        this.sampleDay();
        final ProfitStats day = new ProfitStats();
        if (LocalDate.now().toString().equals(repository().day)) {
            addTo(day, repository().dayStats);
        }
        addTo(day, diff(this.lastSnapshot, this.dayBaseline));
        return new OverallProfit(day, true);
    }

    /** After a session reset: the baseline is the current overall, or the last snapshot while it can't be read. */
    private void rebaseDay() {
        this.sampleDay();
        this.dayBaseline = this.lastSnapshot;
    }

    private static ProfitStats copyOf(ProfitStats stats) {
        final ProfitStats copy = new ProfitStats();
        addTo(copy, stats);
        return copy;
    }

    private static ProfitStats diff(ProfitStats current, ProfitStats baseline) {
        final ProfitStats diff = new ProfitStats();
        diff.profit = current.profit - baseline.profit;
        diff.events = current.events - baseline.events;
        // a session reset by the user (SkyHanni's trackers) can make this negative: no negative time, profit as-is
        diff.uptimeMs = Math.max(0, current.uptimeMs - baseline.uptimeMs);
        return diff;
    }

    /** Records a gain (or a loss, if negative) for an activity and the overall tracker. */
    public void record(@NotNull String activity, double amount) {
        if (!isEnabled()) {
            return;
        }
        final long now = System.currentTimeMillis();
        final long afkTimeout = afkTimeoutMs();
        // overall uptime comes from time on SkyBlock, not from gaps between gains
        this.sessionOverall.add(amount, now, 0);
        repository().overall.add(amount, now, 0);
        this.sessionActivities.computeIfAbsent(activity, name -> new ProfitStats()).add(amount, now, afkTimeout);
        repository().activities.computeIfAbsent(activity, name -> new ProfitStats()).add(amount, now, afkTimeout);
    }

    public @NotNull OverallProfit getOverall(boolean allTime) {
        final ProfitStats own = allTime ? repository().overall : this.sessionOverall;
        if (!this.useSkyHanni()) {
            return new OverallProfit(own, true);
        }
        final List<SkyHanniTrackerReader.TrackerProfit> trackers = this.skyHanni.read();
        if (!this.skyHanni.isAvailable()) {
            return new OverallProfit(own, true);
        }
        // SkyHanni's trackers see every drop (sacks included), SkySens' rare drop messages only some of them,
        // so add up SkyHanni's trackers and the reward chests SkyHanni doesn't track
        final ProfitStats overall = new ProfitStats();
        for (final SkyHanniTrackerReader.TrackerProfit tracker : trackers) {
            addTo(overall, allTime ? tracker.total() : tracker.session());
        }
        for (final String chest : List.of(DUNGEONS, KUUDRA)) {
            addTo(overall, (allTime ? repository().activities : this.sessionActivities).get(chest));
        }
        // all-time SkyHanni totals predate SkySens' time on SkyBlock, so use the trackers' own uptimes there
        if (!allTime) {
            overall.uptimeMs = own.uptimeMs;
        }
        return new OverallProfit(overall, !allTime);
    }

    private static void addTo(ProfitStats target, @Nullable ProfitStats stats) {
        if (stats == null) {
            return;
        }
        target.profit += stats.profit;
        target.events += stats.events;
        target.uptimeMs += stats.uptimeMs;
        target.lastUpdateMs = Math.max(target.lastUpdateMs, stats.lastUpdateMs);
    }

    private boolean useSkyHanni() {
        return SkysensConfig.getInstance().generalCategory.profitTracker.source == ProfitSourceEnum.SKYHANNI
                && this.skyHanni.isAvailable();
    }

    /** The activity that gained most recently, or null if nothing was tracked yet. */
    public @Nullable ActivityProfit getCurrentActivity(boolean allTime) {
        ActivityProfit current = null;
        long currentUpdate = 0;
        final boolean useSkyHanni = this.useSkyHanni();
        if (useSkyHanni) {
            for (final SkyHanniTrackerReader.TrackerProfit tracker : this.skyHanni.read()) {
                final ProfitStats stats = allTime ? tracker.total() : tracker.session();
                if (stats.lastUpdateMs > currentUpdate) {
                    currentUpdate = stats.lastUpdateMs;
                    current = new ActivityProfit(tracker.name(), stats, "SkyHanni");
                }
            }
        }
        for (final Map.Entry<String, ProfitStats> activity : this.sessionActivities.entrySet()) {
            // with SkyHanni installed, only report what SkyHanni doesn't track
            if (useSkyHanni && !isChestActivity(activity.getKey())) {
                continue;
            }
            final ProfitStats session = activity.getValue();
            if (session.lastUpdateMs > currentUpdate) {
                currentUpdate = session.lastUpdateMs;
                final ProfitStats stats = allTime
                        ? repository().activities.getOrDefault(activity.getKey(), session)
                        : session;
                current = new ActivityProfit(activity.getKey(), stats.withLiveUptime(System.currentTimeMillis(), afkTimeoutMs()), "SkySens");
            }
        }
        return current;
    }

    /** Removes the all-time reward chest activities, so they match the reset chest profit. */
    public void resetAllTimeChests() {
        repository().activities.remove(DUNGEONS);
        repository().activities.remove(KUUDRA);
    }

    /** Removes the session reward chest activities, so they match the reset chest profit. */
    public void resetSessionChests() {
        // the chests are part of the session overall, keep what they made in today's profit
        this.foldDay();
        this.sessionActivities.remove(DUNGEONS);
        this.sessionActivities.remove(KUUDRA);
        this.rebaseDay();
    }

    public void resetSession() {
        this.foldDay();
        this.sessionOverall.reset();
        this.sessionActivities.clear();
        this.rebaseDay();
    }

    /** What the player is doing right now, for drops: "Slayer" during a slayer quest, else the island. */
    public static @NotNull String detectActivity() {
        if (ScoreboardUtil.getSidebarLines().stream().anyMatch(line -> line.startsWith("Slayer Quest"))) {
            return "Slayer";
        }
        final SkyblockLocationEnum location = LocationService.getInstance().getCurrentLocation();
        return location == null ? "SkyBlock" : location.getDisplayName();
    }

    public static boolean isChestActivity(String activity) {
        return DUNGEONS.equals(activity) || KUUDRA.equals(activity);
    }

    private static boolean isEnabled() {
        return SkysensConfig.getInstance().generalCategory.profitTracker.enabled;
    }

    private static long afkTimeoutMs() {
        return (long) (SkysensConfig.getInstance().generalCategory.profitTracker.afkTimeoutMinutes * 60_000);
    }

    private static ProfitRepository repository() {
        return RepositoryRegistry.PROFIT_REPOSITORY;
    }

}
