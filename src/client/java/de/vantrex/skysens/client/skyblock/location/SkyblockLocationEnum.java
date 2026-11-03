package de.vantrex.skysens.client.skyblock.location;

import de.vantrex.skysens.client.skyblock.location.zone.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * SkyBlock islands. Constant names are saved in the sensitivity config, so never rename or remove one;
 * islands Hypixel dropped keep their constant without a mode.
 */
@Getter
@RequiredArgsConstructor
public enum SkyblockLocationEnum {

    HUB("Hub Island", "Hub", "hub", HubZoneEnum.class),
    SPIDERS_DEN("Spider's Den", "Spider's Den", "combat_1", SpidersDenZoneEnum.class),
    THE_END("The End", "The End", "combat_3", TheEndZoneEnum.class),
    CRIMSON_ISLE("Crimson Isle", "Crimson Isle", "crimson_isle", CrimsonIsleZoneEnum.class),
    // merged into The Farming Islands
    THE_BARN("The Barn", "The Barn", null, TheBarnZoneEnum.class),
    // merged into The Farming Islands
    MUSHROOM_DESERT("Mushroom Desert", "Mushroom Desert", null, MushroomDesertZoneEnum.class),
    GOLD_MINE("Gold Mine", "Gold Mine", "mining_1", GoldMineZoneEnum.class),
    DEEP_CAVERNS("Deep Caverns", "Deep Caverns", "mining_2", DeepCavernsZoneEnum.class),
    DWARVEN_MINES("Dwarven Mines", "Dwarven Mines", "mining_3", DwarvenMinesZoneEnum.class),
    CRYSTAL_HOLLOWS("Crystal Hollows", "Crystal Hollows", "crystal_hollows", CrystalHollowsZoneEnum.class),
    THE_PARK("The Park", "The Park", "foraging_1", TheParkZoneEnum.class),
    // the island was renamed from Galatea to Moonglade Marsh
    GALATEA("Moonglade Marsh", "Moonglade Marsh", "foraging_2", GalateaZoneEnum.class),
    FISHING_ISLANDS("Backwater Bayou", "Backwater Bayou", "fishing_1", FishingIslandsZoneEnum.class),
    JERRYS_WORKSHOP("Jerry's Workshop", "Jerry's Workshop", "winter", JerrysWorkshopZoneEnum.class),
    RIFT_DIMENSION("The Rift", "The Rift", "rift", RiftZoneEnum.class),
    BLAZING_FORTRESS("Blazing Fortress", "Blazing Fortress", null),
    FLOATING_ISLANDS("Floating Islands", "Floating Islands", null),
    BARTER_BANK_SHOW("Barter Bank Show", "Barter Bank Show", null),
    SNOWBALL_FIGHT_CAVE("Snowball Fight Cave", "Snowball Fight Cave", null),
    OPERATION_MAYHEM("Operation Mayhem", "Operation Mayhem", null),
    PRIVATE_ISLAND("Private Island", "Private Island", "dynamic"),
    THE_GARDEN("The Garden", "Garden", "garden", GardenZoneEnum.class),
    JERRYS_WORKSHOP_JERRY_POND("Jerry's Workshop / Jerry Pond", "Jerry's Workshop / Jerry Pond", null),
    DUNGEON_HUB("Dungeon Hub", "Dungeon Hub", "dungeon_hub"),
    LIMBO("Limbo", "Limbo", null),

    // Farming Islands
    THE_FARMING_ISLANDS("The Farming Islands", "The Farming Islands", "farming_1", FarmingIslandsZoneEnum.class),
    // Instances: dungeon and Kuudra runs
    CATACOMBS("Catacombs", "Dungeon", "dungeon", null, true),
    KUUDRA("Kuudra", "Kuudra", "kuudra", null, true),

    DARK_AUCTION("Dark Auction", "Dark Auction", "dark_auction"),
    MINESHAFT("Glacite Mineshaft", "Mineshaft", "mineshaft"),
    TORRHUS_CANYON("Torrhus Canyon", "Torrhus Canyon", "foraging_3", TorrhusCanyonZoneEnum.class),
    CRITTER_SAFARI("Critter Safari", "Critter Safari", "safari", CritterSafariZoneEnum.class),
    LOTUS_ATOLL("Lotus Atoll", "Lotus Atoll", "lotus_atoll", LotusAtollZoneEnum.class),
    ;

    private final String displayName;
    /** The {@code map} of the Hypixel Mod API location packet. */
    private final String mapName;
    /** The {@code mode} of the Hypixel Mod API location packet, null for islands that no longer exist. */
    private final @Nullable String mode;
    private final Class<? extends ZoneEnum<?>> zoneEnum;
    /** Whether this is a run (dungeon, Kuudra) rather than an island. */
    private final boolean instance;

    SkyblockLocationEnum(final @NonNull String displayName, final String mapName, final @Nullable String mode) {
        this(displayName, mapName, mode, null);
    }

    SkyblockLocationEnum(final @NonNull String displayName, final String mapName, final @Nullable String mode,
                         final Class<? extends ZoneEnum<?>> zoneEnum) {
        this(displayName, mapName, mode, zoneEnum, false);
    }

    /** The display name, as shown in the config dropdown. */
    @Override
    public String toString() {
        return displayName;
    }

    /**
     * The island for a Mod API location packet. The mode is checked first since Hypixel keeps it when an island is
     * renamed; the map name is the fallback. Islands without a mode are never matched.
     */
    public static @Nullable SkyblockLocationEnum fromHypixel(final @Nullable String map, final @Nullable String mode) {
        final SkyblockLocationEnum byMode = find(location -> location.mode.equalsIgnoreCase(mode));
        return byMode != null ? byMode : find(location -> location.mapName.equalsIgnoreCase(map));
    }

    private static @Nullable SkyblockLocationEnum find(final Predicate<SkyblockLocationEnum> matches) {
        for (SkyblockLocationEnum location : values()) {
            if (location.mode != null && matches.test(location)) {
                return location;
            }
        }
        return null;
    }

}
