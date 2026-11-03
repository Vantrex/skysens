package de.vantrex.skysens.client.features.slayer.rng.drop;

import de.vantrex.skysens.client.features.slayer.rng.SlayerRngDrop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The drops and minimum tiers follow the Hypixel SkyBlock wiki's RNG meter template, with Paragon Shard sourced from
 * the boss drop page. Paragon Shard's required Slayer XP follows the wiki formula (500 * 100 / highest drop chance);
 * other XP values are estimates (the wiki marks them outdated and its pages disagree), and the actual value is read
 * from the RNG meter menu.
 * The constant names are stored in the config, don't rename them.
 */
@Getter
@RequiredArgsConstructor
public enum TarantulaRngDropEnum implements SlayerRngDrop {

    NONE("None", 0, 0L),
    BRICK_RED_DYE("Brick Red Dye", 1, 75_000_000L),
    TOXIC_ARROW_POISON("Toxic Arrow Poison", 1, 3_277L),
    BITE_RUNE_I("Bite Rune I", 2, 7_657L),
    DARKNESS_WITHIN_RUNE_I("Darkness Within Rune I", 4, 74_930L),
    SPIDER_CATALYST("Spider Catalyst", 2, 24_250L),
    ENCHANTED_BOOK_BANE_OF_ARTHROPODS_VI("Enchanted Book (Bane of Arthropods VI)", 3, 120_269L),
    TARANTULA_CATALYST("Tarantula Catalyst", 3, 117_108L),
    FLY_SWATTER("Fly Swatter", 3, 234_216L),
    VIAL_OF_VENOM("Vial of Venom", 4, 351_325L),
    TARANTULA_TALISMAN("Tarantula Talisman", 3, 234_216L),
    DIGESTED_MOSQUITO("Digested Mosquito", 4, 702_650L),
    TARANTULA_SILK("Tarantula Silk", 5, 3_513L),
    SHRIVELED_WASP("Shriveled Wasp", 5, 351_325L),
    ENSNARED_SNAIL("Ensnared Snail", 5, 1_171_083L),
    PRIMORDIAL_EYE("Primordial Eye", 5, 3_513_250L),
    PARAGON_SHARD("Paragon Shard", 4, 100_000L);

    private final String itemName;
    private final int minTier;
    private final long wikiRequiredXp;

    @Override
    public String toString() {
        return SlayerRngDrop.label(this);
    }

}
