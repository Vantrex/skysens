package de.vantrex.skysens.client.enums.location.zone;

import de.vantrex.skysens.client.service.Zone;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JerrysWorkshopZoneEnum implements ZoneEnum<JerrysWorkshopZoneEnum> {

    EINARYS_EMPORIUM("Einary's Emporium", "⏣ Einary's Emporium"),
    GARYS_SHACK("Gary's Shack", "⏣ Gary's Shack"),
    GLACIAL_CAVE("Glacial Cave", "⏣ Glacial Cave"),
    HOT_SPRINGS("Hot Springs", "⏣ Hot Springs"),
    JERRY_POND("Jerry Pond", "⏣ Jerry Pond"),
    MOUNT_JERRY("Mount Jerry", "⏣ Mount Jerry"),
    REFLECTIVE_POND("Reflective Pond", "⏣ Reflective Pond"),
    SHERRYS_SHOWROOM("Sherry's Showroom", "⏣ Sherry's Showroom"),
    SUNKEN_JERRY_POND("Sunken Jerry Pond", "⏣ Sunken Jerry Pond"),
    TERRYS_SHACK("Terry's Shack", "⏣ Terry's Shack"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<JerrysWorkshopZoneEnum> getZoneClass() {
        return JerrysWorkshopZoneEnum.class;
    }
}
