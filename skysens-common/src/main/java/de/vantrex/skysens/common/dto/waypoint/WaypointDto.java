package de.vantrex.skysens.common.dto.waypoint;

/**
 * A single waypoint. Deliberately uses plain ints rather than any Minecraft
 * position type — dragging {@code BlockPos} into common is exactly what §4.4
 * forbids. The mod maps this to its own types in {@code client/net/mapper}.
 */
public record WaypointDto(
        String label,
        int x,
        int y,
        int z,
        Integer argbColour
) {
}
