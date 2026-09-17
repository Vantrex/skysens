package de.vantrex.skysens.client.net.cache;

import java.time.Duration;
import java.time.Instant;

/**
 * A value with a TTL and the ETag it was fetched under.
 *
 * <p>Stale is NOT the same as absent. An expired entry is still served to features;
 * expiry only means "worth refreshing in the background". This is what keeps the
 * mod fully functional offline: the last good value never disappears just because
 * it got old.
 */
public final class CachedValue<T> {

    private final T value;
    private final String etag;
    private final Instant fetchedAt;
    private final Duration ttl;

    public CachedValue(final T value, final String etag, final Instant fetchedAt, final Duration ttl) {
        this.value = value;
        this.etag = etag;
        this.fetchedAt = fetchedAt;
        this.ttl = ttl;
    }

    public T value() {
        return value;
    }

    public String etag() {
        return etag;
    }

    /** @return whether a background refresh is worth attempting. */
    public boolean isStale() {
        return Instant.now().isAfter(fetchedAt.plus(ttl));
    }
}
