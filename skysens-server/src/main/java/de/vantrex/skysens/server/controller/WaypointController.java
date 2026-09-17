package de.vantrex.skysens.server.controller;

import de.vantrex.skysens.common.api.WaypointApi;
import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.waypoint.WaypointSetDto;
import de.vantrex.skysens.server.service.WaypointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** §5.3 — community-editable boss waypoint sets. */
@RestController
@RequestMapping("/api/v1/waypoints")
@RequiredArgsConstructor
public class WaypointController implements WaypointApi {

    private final WaypointService waypointService;

    @Override
    @GetMapping
    public PageDto<WaypointSetDto> browse(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        // TODO(deferred): paged list of published waypoint sets
        throw new UnsupportedOperationException("TODO(deferred): waypoint browse");
    }

    @Override
    @GetMapping("/{setId}")
    public WaypointSetDto get(@PathVariable final UUID setId) {
        // TODO(deferred): fetch one set, ETag-cached on its hash
        throw new UnsupportedOperationException("TODO(deferred): waypoint set fetch");
    }

    @Override
    @PostMapping
    public WaypointSetDto publish(@Valid @RequestBody final WaypointSetDto set) {
        // TODO(deferred): persist under the caller's UUID and recompute the hash
        throw new UnsupportedOperationException("TODO(deferred): waypoint publish");
    }
}
