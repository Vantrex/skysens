package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TheParkZoneEnum implements ZoneEnum<TheParkZoneEnum> {

    BIRCH_PARK("Birch Park", "⏣ Birch Park"),
    DARK_THICKET("Dark Thicket", "⏣ Dark Thicket"),
    HOWLING_CAVE("Howling Cave", "⏣ Howling Cave"),
    TRIALS_OF_FIRE("Trials of Fire", "⏣ Trials of Fire"),
    JUNGLE_ISLAND("Jungle Island", "⏣ Jungle Island"),
    SPIRIT_CAVE("Spirit Cave", "⏣ Spirit Cave"),
    SAVANNA_WOODLAND("Savanna Woodland", "⏣ Savanna Woodland"),
    SOUL_CAVE("Soul Cave", "⏣ Soul Cave"),
    MELODYS_PLATEAU("Melody's Plateau", "⏣ Melody's Plateau"),
    SPRUCE_WOODS("Spruce Woods", "⏣ Spruce Woods"),
    LONELY_ISLAND("Lonely Island", "⏣ Lonely Island"),
    VIKING_LONGHOUSE("Viking Longhouse", "⏣ Viking Longhouse"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<TheParkZoneEnum> getZoneClass() {
        return TheParkZoneEnum.class;
    }
}
