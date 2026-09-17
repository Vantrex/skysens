package de.vantrex.skysens.client.net.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link ApiCache} on top of {@code RepositoryRegistry.KEY_VALUE_REPOSITORY}.
 *
 * <p>Uses a dedicated key prefix so API cache entries can be dropped wholesale
 * without disturbing anything else the mod stores there.
 */
public final class RepositoryBackedApiCache implements ApiCache {

    private static final String KEY_PREFIX = "net.cache.";

    @Override
    public <T> Optional<CachedValue<T>> get(final String key, final Class<T> type) {
        // TODO(deferred): read KEY_PREFIX + key from the key/value repository and
        //                 deserialize with the Minecraft-bundled Gson.
        return Optional.empty();
    }

    @Override
    public <T> void put(final String key, final T value, final String etag, final Duration ttl) {
        // TODO(deferred): store value + etag + fetchedAt + ttl under KEY_PREFIX + key.
    }

    @Override
    public void invalidate(final String key) {
        // TODO(deferred): remove KEY_PREFIX + key.
    }

    @Override
    public void clear() {
        // TODO(deferred): remove every key starting with KEY_PREFIX.
    }
}
