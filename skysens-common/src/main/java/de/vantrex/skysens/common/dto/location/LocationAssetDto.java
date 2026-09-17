package de.vantrex.skysens.common.dto.location;

import java.util.List;

/**
 * Wire shape of {@code all_locations.json} (§5.4), served as a versioned asset so
 * zone data can ship without a mod release.
 *
 * <p>These are deliberately NOT
 * {@link de.vantrex.skysens.common.domain.location.SkyblockLocationEnum} —
 * an enum cannot grow new constants at runtime, which is the entire point of
 * serving this remotely. The mod resolves an id to the enum where one exists and
 * ignores ids it does not know.
 */
public record LocationAssetDto(
        String id,
        String name,
        String scoreboardName,
        List<ZoneAssetDto> zones
) {

    public record ZoneAssetDto(
            String id,
            String name,
            String scoreboardName
    ) {
    }
}
