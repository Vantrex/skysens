package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TheBarnZoneEnum implements ZoneEnum<TheBarnZoneEnum> {

    WINDMILL("Windmill", "⏣ Windmill"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<TheBarnZoneEnum> getZoneClass() {
        return TheBarnZoneEnum.class;
    }
}
