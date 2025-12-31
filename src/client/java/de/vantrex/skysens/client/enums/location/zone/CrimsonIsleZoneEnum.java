package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrimsonIsleZoneEnum implements ZoneEnum<CrimsonIsleZoneEnum> {

    AURAS_LAB("Aura's Lab", "⏣ Aura's Lab"),
    BARBARIAN_OUTPOST("Barbarian Outpost", "⏣ Barbarian Outpost"),
    BELLY_OF_THE_BEAST("Belly of the Beast", "⏣ Belly of the Beast"),
    BLAZING_VOLCANO("Blazing Volcano", "⏣ Blazing Volcano"),
    BURNING_DESERT("Burning Desert", "⏣ Burning Desert"),
    CATHEDRAL("Cathedral", "⏣ Cathedral"),
    CHIEFS_HUT("Chief's Hut", "⏣ Chief's Hut"),
    COMMUNITY_CENTER("Community Center", "⏣ Community Center"),
    COURTYARD("Courtyard", "⏣ Courtyard"),
    CRIMSON_FIELDS("Crimson Fields", "⏣ Crimson Fields"),
    DOJO("Dojo", "⏣ Dojo"),
    DRAGONTAIL_AUCTION_HOUSE("Dragontail Auction House", "⏣ Dragontail Auction House"),
    DRAGONTAIL_BANK("Dragontail Bank", "⏣ Dragontail Bank"),
    DRAGONTAIL_BAZAAR("Dragontail Bazaar", "⏣ Dragontail Bazaar"),
    DRAGONTAIL_BLACKSMITH("Dragontail Blacksmith", "⏣ Dragontail Blacksmith"),
    DRAGONTAIL_MINION_SHOP("Dragontail Minion Shop", "⏣ Dragontail Minion Shop"),
    DRAGONTAIL_TOWNSQUARE("Dragontail Townsquare", "⏣ Dragontail Townsquare"),
    DRAGONTAIL("Dragontail", "⏣ Dragontail"),
    FORGOTTEN_SKULL("Forgotten Skull", "⏣ Forgotten Skull"),
    IGRUPANS_CHICKEN_COOP("Igrupan's Chicken Coop", "⏣ Igrupan's Chicken Coop"),
    IGRUPANS_HOUSE("Igrupan's House", "⏣ Igrupan's House"),
    MAGE_COUNCIL("Mage Council", "⏣ Mage Council"),
    MAGE_OUTPOST("Mage Outpost", "⏣ Mage Outpost"),
    MAGMA_CHAMBER("Magma Chamber", "⏣ Magma Chamber"),
    MATRIARCHS_LAIR("Matriarch's Lair", "⏣ Matriarch's Lair"),
    MYSTIC_MARSH("Mystic Marsh", "⏣ Mystic Marsh"),
    ODGERS_HUT("Odger's Hut", "⏣ Odger's Hut"),
    PLHLEGBLAST_POOL("Plhlegblast Pool", "⏣ Plhlegblast Pool"),
    RUINS_OF_ASHFANG("Ruins of Ashfang", "⏣ Ruins of Ashfang"),
    SCARLETON_AUCTION_HOUSE("Scarleton Auction House", "⏣ Scarleton Auction House"),
    SCARLETON_BANK("Scarleton Bank", "⏣ Scarleton Bank"),
    SCARLETON_BAZAAR("Scarleton Bazaar", "⏣ Scarleton Bazaar"),
    SCARLETON_BLACKSMITH("Scarleton Blacksmith", "⏣ Scarleton Blacksmith"),
    SCARLETON_MINION_SHOP("Scarleton Minion Shop", "⏣ Scarleton Minion Shop"),
    SCARLETON_PLAZA("Scarleton Plaza", "⏣ Scarleton Plaza"),
    SCARLETON("Scarleton", "⏣ Scarleton"),
    SMOLDERING_TOMB("Smoldering Tomb", "⏣ Smoldering Tomb"),
    STRONGHOLD("Stronghold", "⏣ Stronghold"),
    THE_BASTION("The Bastion", "⏣ The Bastion"),
    THE_DUKEDOM("The Dukedom", "⏣ The Dukedom"),
    THE_WASTELAND("The Wasteland", "⏣ The Wasteland"),
    THRONE_ROOM("Throne Room", "⏣ Throne Room"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<CrimsonIsleZoneEnum> getZoneClass() {
        return CrimsonIsleZoneEnum.class;
    }

}

