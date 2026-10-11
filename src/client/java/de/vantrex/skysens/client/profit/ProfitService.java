package de.vantrex.skysens.client.profit;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.enums.ProfitSourceEnum;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.util.ScoreboardUtil;
import de.vantrex.skysens.client.repository.ProfitRepository;
import de.vantrex.skysens.client.repository.RepositoryRegistry;
import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Getter
    private static final ProfitService instance = new ProfitService();

    private final SkyHanniTrackerReader skyHanni = new SkyHanniTrackerReader();
    private final ProfitStats sessionOverall = new ProfitStats();
    private final Map<String, ProfitStats> sessionActivities = new HashMap<>();
    private long lastTickMs;

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
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.countUptime());
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
        this.sessionActivities.remove(DUNGEONS);
        this.sessionActivities.remove(KUUDRA);
    }

    public void resetSession() {
        this.sessionOverall.reset();
        this.sessionActivities.clear();
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
