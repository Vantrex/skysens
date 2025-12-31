package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HubZoneEnum implements ZoneEnum<HubZoneEnum> {

    COLOSSEUM("Colosseum", "⏣ Colosseum"),
    COMMUNITY_CENTER("Community Center", "⏣ Community Center"),
    DARK_AUCTION("Dark Auction", "⏣ Dark Auction"),
    ELECTION_ROOM("Election Room", "⏣ Election Room"),
    FARM("Farm", "⏣ Farm"),
    FARMHOUSE("Farmhouse", "⏣ Farmhouse"),
    FASHION_SHOP("Fashion Shop", "⏣ Fashion Shop"),
    FISHERMANS_HUT("Fisherman's Hut", "⏣ Fisherman's Hut"),
    FISHING_OUTPOST("Fishing Outpost", "⏣ Fishing Outpost"),
    FLOWER_HOUSE("Flower House", "⏣ Flower House"),
    FOREST("Forest", "⏣ Forest"),
    GRAVEYARD("Graveyard", "⏣ Graveyard"),
    HUB_CRYPTS("Hub Crypts", "⏣ Hub Crypts"),
    LIBRARY("Library", "⏣ Library"),
    MOUNTAIN("Mountain", "⏣ Mountain"),
    MUSEUM("Museum", "⏣ Museum"),
    RABBIT_HOUSE("Rabbit House", "⏣ Rabbit House"),
    REGALIA_ROOM("Regalia Room", "⏣ Regalia Room"),
    RUINS("Ruins", "⏣ Ruins"),
    SHENS_AUCTION("Shen's Auction", "⏣ Shen's Auction"),
    TAVERN("Tavern", "⏣ Tavern"),
    THAUMATURGIST("Thaumaturgist", "⏣ Thaumaturgist"),
    TRADE_CENTER("Trade Center", "⏣ Trade Center"),
    UNINCORPORATED("Unincorporated", "⏣ Unincorporated"),
    VILLAGE("Village", "⏣ Village"),
    WILDERNESS("Wilderness", "⏣ Wilderness"),
    WIZARD_TOWER("Wizard Tower", "⏣ Wizard Tower"),
    COAL_MINE("Coal Mine", "⏣ Coal Mine"),
    BAZAAR_ALLEY("Bazaar Alley", "⏣ Bazaar Alley"),
    BANK("Bank", "⏣ Bank"),
    AUCTION_HOUSE("Auction House", "⏣ Auction House"),
    BUILDERS_HOUSE("Builder's House", "⏣ Builder's House"),
    CANVAS_ROOM("Canvas Room", "⏣ Canvas Room"),
    ARCHERY_RANGE("Archery Range", "⏣ Archery Range"),
    ABIPHONES_AND_CO("Abiphones & Co.", "⏣ Abiphones & Co."),
    ARTISTS_ABODE("Artist's Abode", "⏣ Artist's Abode"),
    BLACKSMITHS_HOUSE("Blacksmith's House", "⏣ Blacksmith's House"),
    CARNIVAL("Carnival", "⏣ Carnival"),

    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<HubZoneEnum> getZoneClass() {
        return HubZoneEnum.class;
    }
}
