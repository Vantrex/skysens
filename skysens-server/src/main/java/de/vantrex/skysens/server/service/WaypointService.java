package de.vantrex.skysens.server.service;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.waypoint.WaypointSetDto;

import java.util.UUID;

public interface WaypointService {

    PageDto<WaypointSetDto> browse(int page, int size);

    WaypointSetDto get(UUID setId);

    WaypointSetDto publish(UUID authorUuid, WaypointSetDto set);
}
