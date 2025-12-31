package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GalateaZoneEnum implements ZoneEnum<GalateaZoneEnum> {

    MOONGLADE_MARSH("Moonglade Marsh", "⏣ Moonglade Marsh"),
    ANCIENT_RUINS("Ancient Ruins", "⏣ Ancient Ruins"),
    BUBBLEBOOST_COLUMN("Bubbleboost Column", "⏣ Bubbleboost Column"),
    DIVE_EMBER_PASS("Dive-Ember Pass", "⏣ Dive-Ember Pass"),
    DRIPTOAD_DELVE("Driptoad Delve", "⏣ Driptoad Delve"),
    DRIPTOAD_PASS("Driptoad Pass", "⏣ Driptoad Pass"),
    DROWNED_RELIQUARY("Drowned Reliquary", "⏣ Drowned Reliquary"),
    EVERGREEN_PLATEAU("Evergreen Plateau", "⏣ Evergreen Plateau"),
    FOREST_TEMPLE("Forest Temple", "⏣ Forest Temple"),
    FUSION_HOUSE("Fusion House", "⏣ Fusion House"),
    KELPWOVEN_TUNNELS("Kelpwoven Tunnels", "⏣ Kelpwoven Tunnels"),
    MOONGLADES_EDGE("Moonglade's Edge", "⏣ Moonglade's Edge"),
    MURKWATER_DEPTHS("Murkwater Depths", "⏣ Murkwater Depths"),
    MURKWATER_LOCH("Murkwater Loch", "⏣ Murkwater Loch"),
    MURKWATER_OUTPOST("Murkwater Outpost", "⏣ Murkwater Outpost"),
    MURKWATER_SHALLOWS("Murkwater Shallows", "⏣ Murkwater Shallows"),
    NORTH_REACHES("North Reaches", "⏣ North Reaches"),
    NORTH_WETLANDS("North Wetlands", "⏣ North Wetlands"),
    RED_HOUSE("Red House", "⏣ Red House"),
    REEFGUARD_PASS("Reefguard Pass", "⏣ Reefguard Pass"),
    SIDE_EMBER_WAY("Side-Ember Way", "⏣ Side-Ember Way"),
    STRIDE_EMBER_FISSURE("Stride-Ember Fissure", "⏣ Stride-Ember Fissure"),
    SOUTH_REACHES("South Reaches", "⏣ South Reaches"),
    SOUTH_WETLANDS("South Wetlands", "⏣ South Wetlands"),
    SWAMPCUT_INC("SwampCut Inc.", "⏣ SwampCut Inc."),
    TANGLEBURGS_PATH("Tangleburg's Path", "⏣ Tangleburg's Path"),
    TANGLEBURG("Tangleburg", "⏣ Tangleburg"),
    TANGLEBURG_LIBRARY("Tangleburg Library", "⏣ Tangleburg Library"),
    TANGLEBURG_BANK("Tangleburg Bank", "⏣ Tangleburg Bank"),
    TOMB_FLOODWAY("Tomb Floodway", "⏣ Tomb Floodway"),
    TRANQUIL_PASS("Tranquil Pass", "⏣ Tranquil Pass"),
    TRANQUILITY_SANCTUM("Tranquility Sanctum", "⏣ Tranquility Sanctum"),
    VERDANT_SUMMIT("Verdant Summit", "⏣ Verdant Summit"),
    WEST_REACHES("West Reaches", "⏣ West Reaches"),
    WESTBOUND_WETLANDS("Westbound Wetlands", "⏣ Westbound Wetlands"),
    WYRMGROVE_TOMB("Wyrmgrove Tomb", "⏣ Wyrmgrove Tomb"),
    BACKWATER_BAYOU("Backwater Bayou", "⏣ Backwater Bayou"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<GalateaZoneEnum> getZoneClass() {
        return GalateaZoneEnum.class;
    }
}
