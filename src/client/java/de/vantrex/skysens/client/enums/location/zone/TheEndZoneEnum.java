package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TheEndZoneEnum implements ZoneEnum<TheEndZoneEnum> {

    DRAGONS_NEST("Dragon's Nest", "⏣ Dragon's Nest"),
    VOID_SEPULTURE("Void Sepulture", "⏣ Void Sepulture"),
    VOID_SLATE("Void Slate", "⏣ Void Slate"),
    ZEALOT_BRUISER_HIDEOUT("Zealot Bruiser Hideout", "⏣ Zealot Bruiser Hideout"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<TheEndZoneEnum> getZoneClass() {
        return TheEndZoneEnum.class;
    }
}
