package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpidersDenZoneEnum implements ZoneEnum<SpidersDenZoneEnum> {

    ARACHNES_BURROW("Arachne's Burrow", "⏣ Arachne's Burrow"),
    ARACHNES_SANCTUARY("Arachne's Sanctuary", "⏣ Arachne's Sanctuary"),
    ARCHAEOLOGISTS_CAMP("Archaeologist's Camp", "⏣ Archaeologist's Camp"),
    GRANDMAS_HOUSE("Grandma's House", "⏣ Grandma's House"),
    GRAVEL_MINES("Gravel Mines", "⏣ Gravel Mines"),
    SPIDER_MOUND("Spider Mound", "⏣ Spider Mound"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<SpidersDenZoneEnum> getZoneClass() {
        return SpidersDenZoneEnum.class;
    }

}
