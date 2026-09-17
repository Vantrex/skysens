package de.vantrex.skysens.server.service.impl;

import de.vantrex.skysens.common.dto.common.VersionedAssetDto;
import de.vantrex.skysens.common.dto.location.LocationAssetDto;
import de.vantrex.skysens.server.service.LocationAssetService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stub implementation. Every method body is deliberately unimplemented: this pass
 * produces structure only, and zero features are migrated to the server.
 */
@Service
public class LocationAssetServiceImpl implements LocationAssetService {

    @Override
    public VersionedAssetDto getMetadata() {
        // TODO(deferred): hash the asset once at startup and cache it in memory
        throw new UnsupportedOperationException("TODO(deferred): location asset metadata");
    }

    @Override
    public List<LocationAssetDto> getLocations() {
        // TODO(deferred): read classpath:assets/all_locations.json, parse, cache
        throw new UnsupportedOperationException("TODO(deferred): location asset body");
    }
}
