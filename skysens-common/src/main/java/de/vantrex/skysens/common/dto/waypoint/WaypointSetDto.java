package de.vantrex.skysens.common.dto.waypoint;

import de.vantrex.skysens.common.domain.dungeon.DungeonFloorEnum;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Community-editable waypoint set (§5.3), replacing the local data currently
 * baked into {@code BossWaypointsFeature}.
 */
public record WaypointSetDto(
        UUID setId,
        String name,
        DungeonFloorEnum floor,
        String hash,
        Instant updatedAt,
        List<WaypointDto> waypoints
) {
}
