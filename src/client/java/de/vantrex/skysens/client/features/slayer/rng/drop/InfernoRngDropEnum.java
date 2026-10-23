package de.vantrex.skysens.client.features.slayer.rng.drop;

import de.vantrex.skysens.client.features.slayer.rng.SlayerRngDrop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The drops of the Inferno Demonlord RNG meter with their minimum tier, from the Hypixel SkyBlock wiki
 * (Template:RNG_Meter/UI/Slayer). Its required Slayer XP is only an estimate (the wiki marks it outdated and its pages
 * disagree), the real value is read from the RNG meter menu. The constant names are stored in the config, don't
 * rename them.
 */
@Getter
@RequiredArgsConstructor
public enum InfernoRngDropEnum implements SlayerRngDrop {

    NONE("None", 0, 0L),
    FLAME_DYE("Flame Dye", 1, 75_000_000L),
    ENCHANTED_BLAZE_POWDER("Enchanted Blaze Powder", 1, 2_351L),
    LAVATEARS_RUNE_I("Lavatears Rune I", 3, 21_660L),
    WISPS_ICE_FLAVORED_WATER_I_SPLASH_POTION("Wisp's Ice-Flavored Water I Splash Potion", 2, 14_270L),
    BUNDLE_OF_MAGMA_ARROWS("Bundle of Magma Arrows", 2, 4_756L),
    MANA_DISINTEGRATOR("Mana Disintegrator", 2, 10_192L),
    SCORCHED_BOOKS("Scorched Books", 2, 17_837L),
    KELVIN_INVERTER("Kelvin Inverter", 2, 14_270L),
    GABAGOOL_DISTILLATE("Gabagool Distillate", 2, 10_649L),
    SCORCHED_POWER_CRYSTAL("Scorched Power Crystal", 3, 12_558L),
    ARCHFIEND_DICE("Archfiend Dice", 3, 37_675L),
    ENCHANTED_BOOK_FIRE_ASPECT_III("Enchanted Book (Fire Aspect III)", 4, 32_508L),
    FIERY_BURST_RUNE_I("Fiery Burst Rune I", 4, 243_675L),
    FLAWED_OPAL_GEMSTONE("Flawed Opal Gemstone", 4, 14_776L),
    ENCHANTED_BOOK_DUPLEX_I("Enchanted Book (Duplex I)", 4, 23_220L),
    HIGH_CLASS_ARCHFIEND_DICE("High Class Archfiend Dice", 4, 194_939L),
    WILSONS_ENGINEERING_PLANS("Wilson's Engineering Plans", 4, 478_058L),
    SUBZERO_INVERTER("Subzero Inverter", 4, 478_058L);

    private final String itemName;
    private final int minTier;
    private final long wikiRequiredXp;

    @Override
    public String toString() {
        return SlayerRngDrop.label(this);
    }

}
