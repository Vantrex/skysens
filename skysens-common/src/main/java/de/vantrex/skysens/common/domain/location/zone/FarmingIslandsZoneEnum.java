package de.vantrex.skysens.common.domain.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FarmingIslandsZoneEnum implements ZoneEnum<FarmingIslandsZoneEnum> {

    THE_FARMING_ISLANDS("The Farming Islands", "⏣ The Farming Islands"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<FarmingIslandsZoneEnum> getZoneClass() {
        return FarmingIslandsZoneEnum.class;
    }
}

