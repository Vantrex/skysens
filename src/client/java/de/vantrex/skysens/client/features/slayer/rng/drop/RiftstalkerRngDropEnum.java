package de.vantrex.skysens.client.features.slayer.rng.drop;

import de.vantrex.skysens.client.features.slayer.rng.SlayerRngDrop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The drops of the Riftstalker Bloodfiend RNG meter with their minimum tier, from the Hypixel SkyBlock wiki
 * (Template:RNG_Meter/UI/Slayer). Its required Slayer XP is only an estimate (the wiki marks it outdated and its pages
 * disagree), the real value is read from the RNG meter menu. The constant names are stored in the config, don't
 * rename them.
 */
@Getter
@RequiredArgsConstructor
public enum RiftstalkerRngDropEnum implements SlayerRngDrop {

    NONE("None", 0, 0L),
    SANGRIA_DYE("Sangria Dye", 1, 750_000L),
    ENCHANTED_BOOK_BUNDLE_TIER_II("Enchanted Book Bundle", 2, 1_687L),
    SOULTWIST_RUNE_I("Soultwist Rune I", 2, 1_912L),
    BUBBA_BLISTER("Bubba Blister", 2, 2_250L),
    FANG_TASTIC_CHOCOLATE_CHIP("Fang-tastic Chocolate Chip", 2, 2_250L),
    GUARDIAN_LUCKY_BLOCK("Guardian Lucky Block", 3, 3_600L),
    MCGRUBBERS_BURGER("McGrubber's Burger", 4, 18_450L),
    UNFANGED_VAMPIRE_PART("Unfanged Vampire Part", 4, 18_450L),
    ENCHANTED_BOOK_BUNDLE_TIER_V("Enchanted Book Bundle", 5, 12_525L);

    private final String itemName;
    private final int minTier;
    private final long wikiRequiredXp;

    @Override
    public String toString() {
        return SlayerRngDrop.label(this);
    }

}
