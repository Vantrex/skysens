package de.vantrex.skysens.client.skyblock.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CritterSafariZoneEnum implements ZoneEnum<CritterSafariZoneEnum> {

    CAVERN_BIOME("Cavern Biome", "⏣ Cavern Biome"),
    FOREST_BIOME("Forest Biome", "⏣ Forest Biome"),
    HAUNTED_BIOME("Haunted Biome", "⏣ Haunted Biome"),
    ICY_BIOME("Icy Biome", "⏣ Icy Biome"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<CritterSafariZoneEnum> getZoneClass() {
        return CritterSafariZoneEnum.class;
    }
}
