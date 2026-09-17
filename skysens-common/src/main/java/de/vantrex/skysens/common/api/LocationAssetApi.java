package de.vantrex.skysens.common.api;

import de.vantrex.skysens.common.dto.common.VersionedAssetDto;
import de.vantrex.skysens.common.dto.location.LocationAssetDto;

import java.util.List;

/**
 * §5.4 — {@code all_locations.json} as a versioned, ETag-cached public asset.
 */
public interface LocationAssetApi {

    VersionedAssetDto getLocationAssetMetadata();

    List<LocationAssetDto> getLocations();
}
