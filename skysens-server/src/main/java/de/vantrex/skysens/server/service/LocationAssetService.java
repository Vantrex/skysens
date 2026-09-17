package de.vantrex.skysens.server.service;

import de.vantrex.skysens.common.dto.common.VersionedAssetDto;
import de.vantrex.skysens.common.dto.location.LocationAssetDto;

import java.util.List;

public interface LocationAssetService {

    VersionedAssetDto getMetadata();

    List<LocationAssetDto> getLocations();
}
