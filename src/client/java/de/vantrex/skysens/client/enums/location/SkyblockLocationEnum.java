package de.vantrex.skysens.client.enums.location;

import de.vantrex.skysens.client.enums.location.zone.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum SkyblockLocationEnum {

    HUB("Hub Island", "Hub Island", HubZoneEnum.class),
    SPIDERS_DEN("Spider's Den", "⏣ Spider's Den", SpidersDenZoneEnum.class),
    THE_END("The End", "⏣ The End", TheEndZoneEnum.class),
    CRIMSON_ISLE("Crimson Isle", "⏣ Crimson Isle", CrimsonIsleZoneEnum.class),
    THE_BARN("The Barn", "⏣ The Barn", TheBarnZoneEnum.class),
    MUSHROOM_DESERT("Mushroom Desert", "⏣ Mushroom Desert", MushroomDesertZoneEnum.class),
    GOLD_MINE("Gold Mine", "⏣ Gold Mine", GoldMineZoneEnum.class),
    DEEP_CAVERNS("Deep Caverns", "⏣ Deep Caverns", DeepCavernsZoneEnum.class),
    DWARVEN_MINES("Dwarven Mines", "⏣ Dwarven Mines", DwarvenMinesZoneEnum.class),
    CRYSTAL_HOLLOWS("Crystal Hollows", "⏣ Crystal Hollows", CrystalHollowsZoneEnum.class),
    THE_PARK("The Park", "The Park", TheParkZoneEnum.class),
    GALATEA("Galatea", "⏣ Galatea", GalateaZoneEnum.class),
    JERRYS_WORKSHOP("Jerry's Workshop", "⏣ Jerry's Workshop", JerrysWorkshopZoneEnum.class),
    RIFT_DIMENSION("Rift Dimension", "ф Rift Dimension", RiftZoneEnum.class),
    BLAZING_FORTRESS("Blazing Fortress", "⏣ Blazing Fortress"),
    FLOATING_ISLANDS("Floating Islands", "Floating Islands"),
    BARTER_BANK_SHOW("Barter Bank Show", "ф Barter Bank Show"),
    SNOWBALL_FIGHT_CAVE("Snowball Fight Cave", "⏣ Snowball Fight Cave"),
    OPERATION_MAYHEM("Operation Mayhem", "⏣ Operation Mayhem"),
    PRIVATE_ISLAND("Private Island", "⏣ Private Island"),
    THE_GARDEN("The Garden", "⏣ The Garden", GardenZoneEnum.class),
    JERRYS_WORKSHOP_JERRY_POND("Jerry's Workshop / Jerry Pond", "⏣ Jerry's Workshop / ⏣ Jerry Pond"),
    DUNGEON_HUB("Dungeon Hub", "⏣ Dungeon Hub"),
    LIMBO("Limbo", "⏣ Limbo"),

    // Farming Islands
    THE_FARMING_ISLANDS("The Farming Islands", "⏣ The Farming Islands", FarmingIslandsZoneEnum.class),
    CATACOMBS("Catacombs", "Catacombs"),
    ;

    private final String displayName;
    private final String scoreboardName;
    private final Class<? extends ZoneEnum<?>> zoneEnum;

    SkyblockLocationEnum(final @NonNull String displayName, final String scoreboardName) {
        this(displayName, scoreboardName, null);
    }

    public static @Nullable SkyblockLocationEnum fromMode(final String scoreboardName) {
        for (SkyblockLocationEnum location : values()) {
            if (location.getScoreboardName().equals(scoreboardName)) {
                return location;
            }
        }
        return null;
    }

}
