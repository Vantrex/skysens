package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeepCavernsZoneEnum implements ZoneEnum<DeepCavernsZoneEnum>{

    GUNPOWDER_MINES("Gunpowder Mines", "⏣ Gunpowder Mines"),
    LAPIS_QUARRY("Lapis Quarry", "⏣ Lapis Quarry"),
    PIGMENS_DEN("Pigmen's Den", "⏣ Pigmen's Den"),
    SLIMEHILL("Slimehill", "⏣ Slimehill"),
    DIAMOND_RESERVE("Diamond Reserve", "⏣ Diamond Reserve"),
    OBSIDIAN_SANCTUARY("Obsidian Sanctuary", "⏣ Obsidian Sanctuary"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<DeepCavernsZoneEnum> getZoneClass() {
        return DeepCavernsZoneEnum.class;
    }
}
