package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DwarvenMinesZoneEnum implements ZoneEnum<DwarvenMinesZoneEnum> {

    ABANDONED_QUARRY("Abandoned Quarry", "⏣ Abandoned Quarry"),
    CLIFFSIDE_VEINS("Cliffside Veins", "⏣ Cliffside Veins"),
    DIVANS_GATEWAY("Divan's Gateway", "⏣ Divan's Gateway"),
    DWARVEN_BASE_CAMP("Dwarven Base Camp", "⏣ Dwarven Base Camp"),
    DWARVEN_VILLAGE("Dwarven Village", "⏣ Dwarven Village"),
    DWARVEN_TAVERN("Dwarven Tavern", "⏣ Dwarven Tavern"),
    FAR_RESERVE("Far Reserve", "⏣ Far Reserve"),
    FOSSIL_RESEARCH_CENTER("Fossil Research Center", "⏣ Fossil Research Center"),
    GATES_TO_THE_MINES("Gates to the Mines", "⏣ Gates to the Mines"),
    GOBLIN_BURROWS("Goblin Burrows", "⏣ Goblin Burrows"),
    GLACITE_TUNNELS("Glacite Tunnels", "⏣ Glacite Tunnels"),
    GREAT_GLACITE_LAKE("Great Glacite Lake", "⏣ Great Glacite Lake"),
    GREAT_ICE_WALL("Great Ice Wall", "⏣ Great Ice Wall"),
    RAMPARTS_QUARRY("Rampart's Quarry", "⏣ Rampart's Quarry"),
    MINERS_GUILD("Miner's Guild", "⏣ Miner's Guild"),
    ROYAL_MINES("Royal Mines", "⏣ Royal Mines"),
    ROYAL_PALACE("Royal Palace", "⏣ Royal Palace"),
    ARISTOCRAT_PASSAGE("Aristocrat Passage", "⏣ Aristocrat Passage"),
    BARRACKS_OF_HEROES("Barracks of Heroes", "⏣ Barracks of Heroes"),
    GRAND_LIBRARY("Grand Library", "⏣ Grand Library"),
    HANGING_COURT("Hanging Court", "⏣ Hanging Court"),
    PALACE_BRIDGE("Palace Bridge", "⏣ Palace Bridge"),
    ROYAL_QUARTERS("Royal Quarters", "⏣ Royal Quarters"),
    THE_FORGE("The Forge", "⏣ The Forge"),
    FORGE_BASIN("Forge Basin", "⏣ Forge Basin"),
    THE_LIFT("The Lift", "⏣ The Lift"),
    THE_MIST("The Mist", "⏣ The Mist"),
    UPPER_MINES("Upper Mines", "⏣ Upper Mines"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<DwarvenMinesZoneEnum> getZoneClass() {
        return DwarvenMinesZoneEnum.class;
    }
}
