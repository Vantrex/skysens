package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GardenZoneEnum implements ZoneEnum<GardenZoneEnum> {

    THE_GARDEN("The Garden", "⏣ The Garden"),
    FARM("Farm", "⏣ Farm"),
    FARMHOUSE("Farmhouse", "⏣ Farmhouse"),
    BARN("Barn", "⏣ Barn"),
    FISHERMANS_HUT("Fisherman's Hut", "⏣ Fisherman's Hut"),
    FISHING_OUTPOST("Fishing Outpost", "⏣ Fishing Outpost"),
    FOREST("Forest", "⏣ Forest"),
    GRAVEYARD("Graveyard", "⏣ Graveyard"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<GardenZoneEnum> getZoneClass() {
        return GardenZoneEnum.class;
    }
}

