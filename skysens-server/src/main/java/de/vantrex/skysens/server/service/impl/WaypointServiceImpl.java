package de.vantrex.skysens.server.service.impl;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.waypoint.WaypointSetDto;
import de.vantrex.skysens.server.service.WaypointService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub implementation. Every method body is deliberately unimplemented: this pass
 * produces structure only, and zero features are migrated to the server.
 */
@Service
public class WaypointServiceImpl implements WaypointService {

    @Override
    public PageDto<WaypointSetDto> browse(final int page, final int size) {
        // TODO(deferred): paged list of published sets
        throw new UnsupportedOperationException("TODO(deferred): waypoint browse");
    }

    @Override
    public WaypointSetDto get(final UUID setId) {
        // TODO(deferred): fetch one set
        throw new UnsupportedOperationException("TODO(deferred): waypoint set fetch");
    }

    @Override
    public WaypointSetDto publish(final UUID authorUuid, final WaypointSetDto set) {
        // TODO(deferred): persist and recompute the content hash
        throw new UnsupportedOperationException("TODO(deferred): waypoint publish");
    }
}
