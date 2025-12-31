package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GardenZoneEnum implements ZoneEnum<GardenZoneEnum> {

    THE_GARDEN("The Garden", "⏣ The Garden"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<GardenZoneEnum> getZoneClass() {
        return GardenZoneEnum.class;
    }
}

