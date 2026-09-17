package de.vantrex.skysens.client.net.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Local-first cache for everything the API serves, backed by the existing
 * {@code client/repository} layer so cached data persists across restarts through
 * the shutdown hooks already registered in {@code onInitializeClient()}.
 *
 * <p>Read path for every feature is <strong>cache only</strong>. The network never
 * appears on a read path; {@code client/net/sync} refreshes the cache out of band.
 *
 * <p>Bundled resources are the floor: if nothing has ever been fetched, the value
 * falls back to what ships in the jar ({@code splits.json},
 * {@code assets/all_locations.json}). There is no state in which a feature has no
 * data to work with.
 */
public interface ApiCache {

    /** Default TTL for community content. */
    Duration DEFAULT_TTL = Duration.ofHours(6);

    /** Versioned assets change rarely; ETag makes a wasted refresh cheap. */
    Duration ASSET_TTL = Duration.ofHours(24);

    /** The manifest is fetched once per launch and not refreshed. */
    Duration MANIFEST_TTL = Duration.ofDays(365);

    <T> Optional<CachedValue<T>> get(String key, Class<T> type);

    <T> void put(String key, T value, String etag, Duration ttl);

    void invalidate(String key);

    /** Drops everything. Called when the user disables the server or deletes their data. */
    void clear();
}
