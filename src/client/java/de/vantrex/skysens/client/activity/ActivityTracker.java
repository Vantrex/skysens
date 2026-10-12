package de.vantrex.skysens.client.activity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.chat.ActivityAccordion;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.service.LocationService;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Records a timeline of what the player did recently: countable events (dungeon runs, slayer kills, Diana burrows,
 * Kuudra runs) that are merged into one entry per kind, plus island visits that are not already covered by such an
 * entry. The timeline is saved to config/skysens/repositories/ActivityTracker.json so it survives restarts.
 */
public final class ActivityTracker {

    private static final int MAX_ENTRIES = 300;
    private static final long SAVE_INTERVAL_MS = 60_000L;

    @Getter
    private static final ActivityTracker instance = new ActivityTracker();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skysens/repositories/ActivityTracker.json");

    /** One entry of the timeline. Public fields so Gson can persist it. */
    public static final class Entry {
        public ActivityType type;
        public String label;
        public long start;
        public long end;
        public int count;

        public long duration() {
            return Math.max(0, this.end - this.start);
        }
    }

    /** The persisted data. */
    private static final class Data {
        private List<Entry> entries = new ArrayList<>();
    }

    private Data data = new Data();
    private boolean loaded;
    private long lastSave;

    private @Nullable String currentLocation;
    private long currentLocationStart;

    private ActivityTracker() {
    }

    private static ActivityAccordion config() {
        return SkysensConfig.getInstance().chatCommandsCategory.activity;
    }

    /** Records one occurrence of a countable activity that took {@code durationMs} and just ended. */
    public synchronized void record(@NotNull ActivityType type, @NotNull String label, long durationMs) {
        this.load();
        if (!type.isEnabled(config())) {
            return;
        }
        final long now = System.currentTimeMillis();
        final long start = now - Math.max(0, durationMs);
        final long maxGap = (long) (config().mergeGapMinutes * 60_000L);
        // merge into the latest entry of the same kind if it ended recently
        for (int i = this.data.entries.size() - 1; i >= 0; i--) {
            final Entry entry = this.data.entries.get(i);
            if (entry.type == ActivityType.LOCATION) {
                continue;
            }
            if (entry.type == type && entry.label.equals(label) && start - entry.end <= maxGap) {
                entry.count++;
                entry.end = now;
                this.afterChange(now);
                return;
            }
            if (now - entry.end > maxGap) {
                break;
            }
        }
        final Entry entry = new Entry();
        entry.type = type;
        entry.label = label;
        entry.start = start;
        entry.end = now;
        entry.count = 1;
        this.data.entries.add(entry);
        this.afterChange(now);
    }

    /** Called when the player changes island. */
    public synchronized void onLocationChange(@Nullable SkyblockLocationEnum newLocation) {
        this.load();
        this.closeCurrentLocation(System.currentTimeMillis());
        this.currentLocation = newLocation == null ? null : newLocation.getDisplayName();
        this.currentLocationStart = System.currentTimeMillis();
    }

    /** Name of the island the player is on right now, null if unknown. */
    public synchronized @Nullable String currentLocation() {
        if (this.currentLocation == null) {
            final SkyblockLocationEnum location = LocationService.getInstance().getCurrentLocation();
            return location == null ? null : location.getDisplayName();
        }
        return this.currentLocation;
    }

    /** Ends the open island visit and stores it if it was long enough. */
    private void closeCurrentLocation(long now) {
        if (this.currentLocation == null) {
            return;
        }
        final long duration = now - this.currentLocationStart;
        if (ActivityType.LOCATION.isEnabled(config()) && duration >= (long) (config().minSegmentMinutes * 60_000L) && duration > 0) {
            final Entry entry = new Entry();
            entry.type = ActivityType.LOCATION;
            entry.label = this.currentLocation;
            entry.start = this.currentLocationStart;
            entry.end = now;
            entry.count = 1;
            this.data.entries.add(entry);
            this.afterChange(now);
        }
        this.currentLocation = null;
    }

    /** The latest activities, most recent first. Island visits that overlap a counted activity are left out. */
    public synchronized @NotNull List<Entry> recent(int limit) {
        this.load();
        final ActivityAccordion config = config();
        final long now = System.currentTimeMillis();
        final List<Entry> events = new ArrayList<>();
        final List<Entry> result = new ArrayList<>();
        for (final Entry entry : this.data.entries) {
            if (entry.type != ActivityType.LOCATION && entry.type.isEnabled(config)) {
                events.add(entry);
            }
        }
        for (final Entry entry : this.data.entries) {
            if (!entry.type.isEnabled(config)) {
                continue;
            }
            if (entry.type == ActivityType.LOCATION && events.stream().anyMatch(event -> event.start < entry.end && event.end > entry.start)) {
                continue;
            }
            result.add(entry);
        }
        result.sort(Comparator.comparingLong((Entry entry) -> entry.end).reversed());
        return new ArrayList<>(result.subList(0, Math.min(limit, result.size())));
    }

    /** Milliseconds the player has been on the current island. */
    public synchronized long timeAtCurrentLocation() {
        return this.currentLocation == null ? 0 : System.currentTimeMillis() - this.currentLocationStart;
    }

    private void afterChange(long now) {
        this.prune(now);
        if (now - this.lastSave >= SAVE_INTERVAL_MS) {
            this.save();
        }
    }

    private void prune(long now) {
        final long oldest = now - (long) (config().historyHours * 3_600_000L);
        this.data.entries.removeIf(entry -> entry.end < oldest);
        while (this.data.entries.size() > MAX_ENTRIES) {
            this.data.entries.removeFirst();
        }
    }

    private void load() {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        try {
            if (Files.exists(FILE)) {
                final Data read = GSON.fromJson(Files.readString(FILE), Data.class);
                if (read != null && read.entries != null) {
                    read.entries.removeIf(entry -> entry == null || entry.type == null || entry.label == null);
                    this.data = read;
                }
            }
        } catch (IOException | RuntimeException e) {
            this.data = new Data();
        }
    }

    /** Stores the island visit that is still open and saves the timeline. Runs on shutdown. */
    public synchronized void shutdown() {
        if (!this.loaded) {
            return;
        }
        this.closeCurrentLocation(System.currentTimeMillis());
        this.save();
    }

    private void save() {
        this.lastSave = System.currentTimeMillis();
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this.data));
        } catch (IOException ignored) {
            // the timeline is a convenience, losing it is not worth crashing for
        }
    }

}
