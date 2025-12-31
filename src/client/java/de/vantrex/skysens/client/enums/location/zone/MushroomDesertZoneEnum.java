package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MushroomDesertZoneEnum implements ZoneEnum<MushroomDesertZoneEnum>{

    DESERT_MOUNTAIN("Desert Mountain", "⏣ Desert Mountain"),
    DESERT_SETTLEMENT("Desert Settlement", "⏣ Desert Settlement"),
    GLOWING_MUSHROOM_CAVE("Glowing Mushroom Cave", "⏣ Glowing Mushroom Cave"),
    JAKES_HOUSE("Jake's House", "⏣ Jake's House"),
    OASIS("Oasis", "⏣ Oasis"),
    OVERGROWN_MUSHROOM_CAVE("Overgrown Mushroom Cave", "⏣ Overgrown Mushroom Cave"),
    MUSHROOM_GORGE("Mushroom Gorge", "⏣ Mushroom Gorge"),
    SHEPHERDS_KEEP("Shepherd's Keep", "⏣ Shepherd's Keep"),
    TRAPPERS_DEN("Trapper's Den", "⏣ Trapper's Den"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<MushroomDesertZoneEnum> getZoneClass() {
        return MushroomDesertZoneEnum.class;
    }
}
