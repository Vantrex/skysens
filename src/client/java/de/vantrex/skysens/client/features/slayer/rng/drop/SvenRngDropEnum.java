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
public enum SvenRngDropEnum implements SlayerRngDrop {

    NONE("None", 0, 0L),
    CELESTE_DYE("Celeste Dye", 1, 75_000_000L),
    HAMSTER_WHEEL("Hamster Wheel", 2, 3_000L),
    SPIRIT_RUNE_I("Spirit Rune I", 2, 7_917L),
    ENCHANTED_BOOK_CRITICAL_VI("Enchanted Book (Critical VI)", 3, 61_634L),
    FURBALL("Furball", 3, 30_637L),
    RED_CLAW_EGG("Red Claw Egg", 3, 410_900L),
    COUTURE_RUNE_I("Couture Rune I", 4, 219_833L),
    OVERFLUX_CAPACITOR("Overflux Capacitor", 4, 1_232_700L),
    GRIZZLY_SALMON("Grizzly Salmon", 4, 880_500L),
    PARAGON_SHARD("Paragon Shard", 4, 200_000L);

    private final String itemName;
    private final int minTier;
    private final long wikiRequiredXp;

    @Override
    public String toString() {
        return SlayerRngDrop.label(this);
    }

}
