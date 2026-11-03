package de.vantrex.skysens.client.skyblock.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LotusAtollZoneEnum implements ZoneEnum<LotusAtollZoneEnum> {

    LOTUS_EATERS_CAVE("Lotus Eater's Cave", "⏣ Lotus Eater's Cave"),
    LOTUS_HIGHLANDS("Lotus Highlands", "⏣ Lotus Highlands"),
    TEWTIL_TUNNEL("Tewtil Tunnel", "⏣ Tewtil Tunnel"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<LotusAtollZoneEnum> getZoneClass() {
        return LotusAtollZoneEnum.class;
    }
}
