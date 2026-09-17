package de.vantrex.skysens.server.mapper;

import org.springframework.stereotype.Component;

/**
 * Entity <-> common DTO translation for the Waypoint domain.
 *
 * <p>This layer exists so the storage schema and the wire contract can move
 * independently. Nothing outside {@code mapper} may touch both sides.
 */
@Component
public class WaypointMapper {

    // TODO(deferred): toDto / toEntity once the entities have fields.
}
