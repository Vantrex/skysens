package de.vantrex.skysens.common.domain.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TheBarnZoneEnum implements ZoneEnum<TheBarnZoneEnum> {

    WINDMILL("Windmill", "⏣ Windmill"),
    THE_BARN("The Barn", "⏣ The Barn"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<TheBarnZoneEnum> getZoneClass() {
        return TheBarnZoneEnum.class;
    }
}
