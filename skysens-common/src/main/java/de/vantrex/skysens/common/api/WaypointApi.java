package de.vantrex.skysens.common.api;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.waypoint.WaypointSetDto;

import java.util.UUID;

/**
 * §5.3 — community-editable boss waypoint sets.
 */
public interface WaypointApi {

    PageDto<WaypointSetDto> browse(int page, int size);

    WaypointSetDto get(UUID setId);

    WaypointSetDto publish(WaypointSetDto set);
}
