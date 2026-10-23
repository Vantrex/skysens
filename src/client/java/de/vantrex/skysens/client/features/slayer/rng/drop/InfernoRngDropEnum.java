package de.vantrex.skysens.client.features.slayer.rng.drop;

import de.vantrex.skysens.client.features.slayer.rng.SlayerRngDrop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The drops and minimum tiers follow the Inferno Demonlord boss page on the Hypixel SkyBlock wiki. Required Slayer XP
 * values are estimates from its rounded drop chances, except High Class Archfiend Dice, which is confirmed in-game.
 * The actual value is read from the RNG meter menu.
 * The constant names are stored in the config, don't rename them.
 */
@Getter
@RequiredArgsConstructor
public enum InfernoRngDropEnum implements SlayerRngDrop {

    NONE("None", 0, 0L),
    FLAME_DYE("Flame Dye", 1, 75_000_000L),
    ENCHANTED_BLAZE_POWDER("Enchanted Blaze Powder", 1, 3_834L),
    LAVATEARS_RUNE_I("Lavatears Rune I", 3, 21_645L),
    WISPS_ICE_FLAVORED_WATER_I_SPLASH_POTION("Wisp's Ice-Flavored Water I Splash Potion", 2, 16_181L),
    BUNDLE_OF_MAGMA_ARROWS("Bundle of Magma Arrows", 2, 5_400L),
    MANA_DISINTEGRATOR("Mana Disintegrator", 2, 11_574L),
    SCORCHED_BOOKS("Scorched Books", 2, 20_243L),
    INFERNO_DEMONLORD_SHARD("Inferno Demonlord Shard", 2, 9_488L),
    KELVIN_INVERTER("Kelvin Inverter", 2, 16_181L),
    GABAGOOL_DISTILLATE("Gabagool Distillate", 2, 9_634L),
    SCORCHED_POWER_CRYSTAL("Scorched Power Crystal", 3, 14_368L),
    ARCHFIEND_DICE("Archfiend Dice", 3, 43_103L),
    ENCHANTED_BOOK_FIRE_ASPECT_III("Enchanted Book (Fire Aspect III)", 4, 37_879L),
    FIERY_BURST_RUNE_I("Fiery Burst Rune I", 4, 238_095L),
    FLAWED_OPAL_GEMSTONE("Flawed Opal Gemstone", 4, 17_241L),
    ENCHANTED_BOOK_DUPLEX_I("Enchanted Book (Duplex I)", 4, 27_174L),
    HIGH_CLASS_ARCHFIEND_DICE("High Class Archfiend Dice", 4, 188_100L),
    WILSONS_ENGINEERING_PLANS("Wilson's Engineering Plans", 4, 555_556L),
    SUBZERO_INVERTER("Subzero Inverter", 4, 555_556L),
    PARAGON_SHARD("Paragon Shard", 4, 200_000L);

    private final String itemName;
    private final int minTier;
    private final long wikiRequiredXp;

    @Override
    public String toString() {
        return SlayerRngDrop.label(this);
    }

}
