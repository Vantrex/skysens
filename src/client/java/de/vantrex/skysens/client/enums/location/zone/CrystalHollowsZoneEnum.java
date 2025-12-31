package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrystalHollowsZoneEnum implements ZoneEnum<CrystalHollowsZoneEnum>{

    CRYSTAL_NUCLEUS("Crystal Nucleus", "⏣ Crystal Nucleus"),
    DRAGONS_LAIR("Dragon's Lair", "⏣ Dragon's Lair"),
    FAIRY_GROTTO("Fairy Grotto", "⏣ Fairy Grotto"),
    GOBLIN_HOLDOUT("Goblin Holdout", "⏣ Goblin Holdout"),
    GOBLIN_QUEENS_DEN("Goblin Queen's Den", "⏣ Goblin Queen's Den"),
    JUNGLE("Jungle", "⏣ Jungle"),
    JUNGLE_TEMPLE("Jungle Temple", "⏣ Jungle Temple"),
    MAGMA_FIELDS("Magma Fields", "⏣ Magma Fields"),
    KHAZAD_DUM("Khazad-dum", "⏣ Khazad-dûm"),
    MITHRIL_DEPOSITS("Mithril Deposits", "⏣ Mithril Deposits"),
    MINES_OF_DIVAN("Mines of Divan", "⏣ Mines of Divan"),
    PRECURSOR_REMNANTS("Precursor Remnants", "⏣ Precursor Remnants"),
    LOST_PRECURSOR_CITY("Lost Precursor City", "⏣ Lost Precursor City"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<CrystalHollowsZoneEnum> getZoneClass() {
        return CrystalHollowsZoneEnum.class;
    }

}
