package de.vantrex.skysens.common.dto.common;

import java.time.Instant;

/**
 * NEU-repo style versioned asset header.
 *
 * <p>The mod stores {@link #hash()} alongside its cached copy and sends it as
 * {@code If-None-Match}; the server answers {@code 304 NOT MODIFIED} when nothing
 * changed. This is what lets zone data ship without a mod release (§5.4).
 *
 * @param assetId  stable identifier, e.g. {@code all_locations}
 * @param hash     content hash, also served as the ETag
 * @param updatedAt when the asset last changed server-side
 * @param sizeBytes payload size, so the client can refuse absurd downloads
 */
public record VersionedAssetDto(
        String assetId,
        String hash,
        Instant updatedAt,
        long sizeBytes
) {
}
