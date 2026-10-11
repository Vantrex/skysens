package de.vantrex.skysens.client.profit;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads SkyHanni's item profit trackers (Slayer, Fishing, Diana, ...) via reflection, so {@code !profit} can
 * report SkyHanni's numbers for the current activity. Profit is computed like SkyHanni's display: every
 * non-hidden item times SkyHanni's own price for it (coins are an item priced at 1), minus the
 * slayer spawn costs.
 * <p>
 * SkyHanni's NeuInternalName, SimpleTimeMark and kotlin Duration are value classes, so on the JVM they are
 * String/long and methods using them have mangled names ({@code getPricePer-abc123}); those are found by prefix.
 */
final class SkyHanniTrackerReader {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");
    private static final String PACKAGE = "at.hannibal2.skyhanni.";
    /** SkyHanni objects holding item profit trackers, with the activity name we show. */
    private static final Map<String, String> TRACKER_HOLDERS = Map.ofEntries(
            Map.entry("features.slayer.SlayerProfitTracker", "Slayer"),
            Map.entry("features.fishing.tracker.FishingProfitTracker", "Fishing"),
            Map.entry("features.event.diana.DianaProfitTracker", "Diana"),
            Map.entry("features.combat.ghosttracker.GhostTracker", "Ghosts"),
            Map.entry("features.gifting.GiftProfitTracker", "Gifts"),
            Map.entry("features.hunting.HuntingProfitTracker", "Hunting"),
            Map.entry("features.inventory.experimentationtable.ExperimentsProfitTracker", "Experiments"),
            Map.entry("features.mining.crystalhollows.CrystalNucleusTracker", "Crystal Nucleus"),
            Map.entry("features.mining.fossilexcavator.ExcavatorProfitTracker", "Fossil Excavator"),
            Map.entry("features.minion.InfernoMinionProfitTracker", "Inferno Minion"),
            Map.entry("features.garden.tracker.PestProfitTracker", "Pests"),
            Map.entry("features.misc.DraconicSacrificeTracker", "Draconic Sacrifice"),
            Map.entry("features.event.yearofthepig.ShinyOrbTracker", "Shiny Orbs"),
            Map.entry("features.rift.area.mountaintop.TimiteTracker", "Timite"),
            Map.entry("features.combat.end.DragonProfitTracker", "Dragons"),
            Map.entry("features.mining.glacitemineshaft.corpse.CorpseTracker", "Corpses"),
            Map.entry("features.foraging.ForagingTracker", "Foraging"),
            Map.entry("features.garden.tracker.CropFeverTracker", "Crop Fever"),
            Map.entry("features.mining.DarkMonolithFeatures", "Dark Monolith")
    );

    /** A SkyHanni tracker with its session and total profit. */
    record TrackerProfit(String name, ProfitStats session, ProfitStats total) {
    }

    private final boolean loaded = FabricLoader.getInstance().isModLoaded("skyhanni");
    private boolean broken;
    private Class<?> trackerClass;
    private Field lastUpdateField;
    private Method sharedTrackerGetter;
    private Object sessionMode;
    private Object totalMode;

    boolean isAvailable() {
        return this.loaded && !this.broken;
    }

    /**
     * All SkyHanni trackers with their all-time total, including slayer types not fought this game launch (they
     * only exist in SkyHanni's storage). The session of a tracker that wasn't updated this game launch is empty
     * and its {@code lastUpdateMs} is 0.
     */
    synchronized @NotNull List<TrackerProfit> read() {
        final List<TrackerProfit> result = new ArrayList<>();
        if (!this.isAvailable() || !this.resolve()) {
            return result;
        }
        // a tracker may be reachable twice (e.g. a "current" field and a map), it must only be summed once
        final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        // the total data of every tracker read, so storage entries backing a live tracker aren't summed twice
        final Set<Object> seenData = Collections.newSetFromMap(new IdentityHashMap<>());
        Object pricingTracker = null;
        for (final Map.Entry<String, String> holder : TRACKER_HOLDERS.entrySet()) {
            try {
                final Object instance = Class.forName(PACKAGE + holder.getKey()).getField("INSTANCE").get(null);
                for (final Map.Entry<String, Object> tracker : this.findTrackers(instance, holder.getValue()).entrySet()) {
                    if (!seen.add(tracker.getValue())) {
                        continue;
                    }
                    if (pricingTracker == null || holder.getKey().endsWith("SlayerProfitTracker")) {
                        pricingTracker = tracker.getValue();
                    }
                    final TrackerProfit profit = this.readTracker(tracker.getKey(), tracker.getValue(), seenData);
                    if (profit != null) {
                        result.add(profit);
                    }
                }
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                // this tracker doesn't exist in the installed SkyHanni version
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                LOGGER.debug("Could not read SkyHanni tracker {}", holder.getKey(), e);
            }
        }
        this.readStoredSlayers(result, seenData, pricingTracker);
        return result;
    }

    /** SkyHanni's storage of the current profile, only useful for telling profiles apart by identity. */
    @Nullable Object currentProfile() {
        try {
            final Object storage = Class.forName(PACKAGE + "data.ProfileStorageData").getField("INSTANCE").get(null);
            return storage.getClass().getMethod("getProfileSpecific").invoke(storage);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }

    /** Slayer types not fought this game launch have no tracker, only data in SkyHanni's profile storage. */
    private void readStoredSlayers(List<TrackerProfit> result, Set<Object> seenData, @Nullable Object pricingTracker) {
        if (pricingTracker == null) {
            return;
        }
        try {
            final Object storage = Class.forName(PACKAGE + "data.ProfileStorageData").getField("INSTANCE").get(null);
            final Object profileSpecific = storage.getClass().getMethod("getProfileSpecific").invoke(storage);
            if (profileSpecific == null) {
                return;
            }
            final Object stored = profileSpecific.getClass().getMethod("getSlayerProfitData").invoke(profileSpecific);
            if (!(stored instanceof Map<?, ?> map)) {
                return;
            }
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() == null || !seenData.add(entry.getValue())) {
                    continue;
                }
                final ProfitStats total = this.readData(pricingTracker, entry.getValue());
                if (total != null) {
                    result.add(new TrackerProfit("Slayer (" + entry.getKey() + ")", new ProfitStats(), total));
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOGGER.debug("Could not read SkyHanni's stored slayer profits", e);
        }
    }

    private boolean resolve() {
        if (this.trackerClass != null) {
            return true;
        }
        try {
            this.trackerClass = Class.forName(PACKAGE + "utils.tracker.SkyHanniTracker");
            this.lastUpdateField = this.trackerClass.getDeclaredField("lastUpdate");
            this.lastUpdateField.setAccessible(true);
            this.sharedTrackerGetter = this.trackerClass.getDeclaredMethod("getSharedTracker");
            this.sharedTrackerGetter.setAccessible(true);
            final Class<?> displayMode = Class.forName(PACKAGE + "utils.tracker.SkyHanniTracker$DisplayMode");
            this.sessionMode = displayMode.getField("SESSION").get(null);
            this.totalMode = displayMode.getField("TOTAL").get(null);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            this.broken = true;
            this.trackerClass = null;
            LOGGER.warn("Could not read SkyHanni's profit trackers, using SkySens' own tracker", e);
            return false;
        }
    }

    /** The trackers held by a SkyHanni object: itself, tracker fields, or maps of trackers (e.g. per slayer type). */
    private Map<String, Object> findTrackers(Object holder, String name) throws IllegalAccessException {
        final Map<String, Object> trackers = new java.util.LinkedHashMap<>();
        if (this.trackerClass.isInstance(holder)) {
            trackers.put(name, holder);
            return trackers;
        }
        for (final Field field : holder.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == holder.getClass()) {
                continue;
            }
            field.setAccessible(true);
            final Object value = field.get(holder);
            if (this.trackerClass.isInstance(value)) {
                trackers.put(name, value);
            } else if (value instanceof Map<?, ?> map) {
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    if (this.trackerClass.isInstance(entry.getValue())) {
                        // e.g. slayer: one tracker per slayer type
                        trackers.put(name + " (" + entry.getKey() + ")", entry.getValue());
                    }
                }
            }
        }
        return trackers;
    }

    private @Nullable TrackerProfit readTracker(String name, Object tracker, Set<Object> seenData) throws ReflectiveOperationException {
        // lastUpdate stays far in the past until the tracker gains something this game launch
        final long lastUpdate = this.lastUpdateField.getLong(tracker);
        final boolean updated = lastUpdate > 0 && lastUpdate != Long.MIN_VALUE;
        final Object shared = this.sharedTrackerGetter.invoke(tracker);
        if (shared == null) {
            return null;
        }
        final Method get = findMethod(shared.getClass(), "get", 1);
        if (get == null) {
            return null;
        }
        final Object totalData = get.invoke(shared, this.totalMode);
        if (totalData == null || !seenData.add(totalData)) {
            return null;
        }
        final ProfitStats total = this.readData(tracker, totalData);
        final ProfitStats session = updated ? this.readData(tracker, get.invoke(shared, this.sessionMode)) : new ProfitStats();
        if (session == null || total == null) {
            return null;
        }
        // lastUpdateMs 0 keeps a stale tracker from being picked as the current activity
        session.lastUpdateMs = updated ? lastUpdate : 0;
        total.lastUpdateMs = updated ? lastUpdate : 0;
        return new TrackerProfit(name, session, total);
    }

    private @Nullable ProfitStats readData(Object tracker, Object data) throws ReflectiveOperationException {
        if (data == null) {
            return null;
        }
        final Map<?, ?> items = items(data);
        if (items == null) {
            return null;
        }
        final Method customPrice = findMethod(data.getClass(), "getCustomPricePer", 2);
        final Method pricePer = findMethod(tracker.getClass(), "getPricePer", 1);
        final ProfitStats stats = new ProfitStats();
        for (final Map.Entry<?, ?> entry : items.entrySet()) {
            final Object item = entry.getValue();
            if (Boolean.TRUE.equals(invokeGetter(item, "getHidden"))) {
                continue;
            }
            final Object amount = invokeGetter(item, "getTotalAmount");
            if (!(amount instanceof Long count) || count == 0) {
                continue;
            }
            final String internalName = unbox(entry.getKey());
            final Object price = customPrice != null
                    ? customPrice.invoke(data, internalName, tracker)
                    : pricePer != null ? pricePer.invoke(tracker, internalName) : null;
            if (price instanceof Double perItem) {
                stats.profit += perItem * count;
            }
            stats.events += count;
        }
        // the slayer tracker keeps its spawn costs outside the items (stored negative, abs() to be safe)
        if (invokeGetter(data, "getSlayerSpawnCost") instanceof Long spawnCost) {
            stats.profit -= Math.abs(spawnCost);
        }
        final Method uptime = findMethod(data.getClass(), "getTotalUptime", 0);
        if (uptime != null && uptime.invoke(data) instanceof Long rawDuration) {
            stats.uptimeMs = durationToMillis(rawDuration);
        }
        return stats;
    }

    private static @Nullable Map<?, ?> items(Object data) throws ReflectiveOperationException {
        // bucketed trackers (pests) keep items per bucket, use all of them, not only the one selected in the display
        final Method flatten = findMethod(data.getClass(), "flattenBucketsItems", 0);
        if (flatten != null && flatten.invoke(data) instanceof Map<?, ?> all) {
            return all;
        }
        final Object bucketed = invokeGetter(data, "getSelectedBucketItems");
        if (bucketed instanceof Map<?, ?> map) {
            return map;
        }
        return invokeGetter(data, "getItems") instanceof Map<?, ?> map ? map : null;
    }

    /** Unboxes a NeuInternalName value class to its String. */
    private static String unbox(Object internalName) throws ReflectiveOperationException {
        if (internalName instanceof String string) {
            return string;
        }
        return (String) internalName.getClass().getMethod("unbox-impl").invoke(internalName);
    }

    /** Decodes kotlin.time.Duration's raw long: nanoseconds or milliseconds, marked by the lowest bit. */
    private static long durationToMillis(long raw) {
        final long value = raw >> 1;
        return (raw & 1) == 0 ? value / 1_000_000 : value;
    }

    private static @Nullable Object invokeGetter(Object target, String name) throws ReflectiveOperationException {
        try {
            final Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static @Nullable Method findMethod(Class<?> owner, String prefix, int parameterCount) {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            for (final Method method : type.getDeclaredMethods()) {
                if ((method.getName().equals(prefix) || method.getName().startsWith(prefix + "-"))
                        && method.getParameterCount() == parameterCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

}
