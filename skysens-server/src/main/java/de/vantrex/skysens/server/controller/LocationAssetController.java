package de.vantrex.skysens.server.controller;

import de.vantrex.skysens.common.api.LocationAssetApi;
import de.vantrex.skysens.common.dto.common.VersionedAssetDto;
import de.vantrex.skysens.common.dto.location.LocationAssetDto;
import de.vantrex.skysens.server.service.LocationAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * §5.4 — {@code all_locations.json} served NEU-repo style.
 *
 * <p>Public and unauthenticated: this is reference data, not user data. The
 * authoritative copy is {@code src/main/resources/assets/all_locations.json}; the
 * mod ships its own snapshot as the offline fallback and only refetches when the
 * hash differs.
 */
@RestController
@RequestMapping("/api/v1/assets/locations")
@RequiredArgsConstructor
public class LocationAssetController implements LocationAssetApi {

    private final LocationAssetService locationAssetService;

    @Override
    @GetMapping("/meta")
    public VersionedAssetDto getLocationAssetMetadata() {
        // TODO(deferred): return hash + updatedAt so the client can skip the body
        throw new UnsupportedOperationException("TODO(deferred): location asset metadata");
    }

    @Override
    @GetMapping
    public List<LocationAssetDto> getLocations() {
        // TODO(deferred): serve the asset with an ETag; honour If-None-Match -> 304
        throw new UnsupportedOperationException("TODO(deferred): location asset body");
    }
}
