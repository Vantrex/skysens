package de.vantrex.skysens.client.skyblock.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FarmingIslandsZoneEnum implements ZoneEnum<FarmingIslandsZoneEnum> {

    THE_FARMING_ISLANDS("The Farming Islands", "⏣ The Farming Islands"),
    // The Barn
    THE_BARN("The Barn", "⏣ The Barn"),
    WINDMILL("Windmill", "⏣ Windmill"),
    // Mushroom Desert
    ARCHAEOLOGICAL_SITE("Archaeological Site", "⏣ Archaeological Site"),
    DESERT_MOUNTAIN("Desert Mountain", "⏣ Desert Mountain"),
    DESERT_SETTLEMENT("Desert Settlement", "⏣ Desert Settlement"),
    GLOWING_MUSHROOM_CAVE("Glowing Mushroom Cave", "⏣ Glowing Mushroom Cave"),
    JAKES_HOUSE("Jake's House", "⏣ Jake's House"),
    MUSHROOM_GORGE("Mushroom Gorge", "⏣ Mushroom Gorge"),
    OASIS("Oasis", "⏣ Oasis"),
    OVERGROWN_MUSHROOM_CAVE("Overgrown Mushroom Cave", "⏣ Overgrown Mushroom Cave"),
    SHEPHERDS_KEEP("Shepherd's Keep", "⏣ Shepherd's Keep"),
    TRAPPERS_DEN("Trapper's Den", "⏣ Trapper's Den"),
    UNDERGROUND_LAB("Underground Lab", "⏣ Underground Lab"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<FarmingIslandsZoneEnum> getZoneClass() {
        return FarmingIslandsZoneEnum.class;
    }
}

