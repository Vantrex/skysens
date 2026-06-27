package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FishingIslandsZoneEnum implements ZoneEnum<FishingIslandsZoneEnum> {

    BACKWATER_BAYOU("Backwater Bayou", "⏣ Backwater Bayou"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<FishingIslandsZoneEnum> getZoneClass() {
        return FishingIslandsZoneEnum.class;
    }
}
