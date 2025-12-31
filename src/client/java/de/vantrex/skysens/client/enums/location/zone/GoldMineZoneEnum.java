package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoldMineZoneEnum implements ZoneEnum<GoldMineZoneEnum> {

    GOLD_MINE("Gold Mine", "⏣ Gold Mine")
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<GoldMineZoneEnum> getZoneClass() {
        return GoldMineZoneEnum.class;
    }
}
