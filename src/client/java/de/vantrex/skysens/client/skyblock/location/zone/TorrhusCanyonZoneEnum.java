package de.vantrex.skysens.client.skyblock.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TorrhusCanyonZoneEnum implements ZoneEnum<TorrhusCanyonZoneEnum> {

    ANTS_CAVE("Ant's Cave", "⏣ Ant's Cave"),
    CRITTER_SAFARI_ENTRANCE("Critter Safari Entrance", "⏣ Critter Safari Entrance"),
    DESERT_TEMPLE("Desert Temple", "⏣ Desert Temple"),
    HOTSPOT_HAVEN("Hotspot Haven", "⏣ Hotspot Haven"),
    MIRIAS_HUT("Miria's Hut", "⏣ Miria's Hut"),
    PANGOLIN_HIDEAWAY("Pangolin Hideaway", "⏣ Pangolin Hideaway"),
    SPRING_DEPTHS("Spring Depths", "⏣ Spring Depths"),
    SPRING_PATH("Spring Path", "⏣ Spring Path"),
    SPRING_SHALLOWS("Spring Shallows", "⏣ Spring Shallows"),
    TORRHUS_HEIGHTS("Torrhus Heights", "⏣ Torrhus Heights"),
    TORRHUS_SPRINGS("Torrhus Springs", "⏣ Torrhus Springs"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<TorrhusCanyonZoneEnum> getZoneClass() {
        return TorrhusCanyonZoneEnum.class;
    }
}
