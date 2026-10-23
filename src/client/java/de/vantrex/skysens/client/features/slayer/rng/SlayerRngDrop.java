package de.vantrex.skysens.client.features.slayer.rng;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** A drop of a slayer RNG meter. Implemented by one enum per slayer, whose first constant {@code NONE} means no drop. */
public interface SlayerRngDrop {

    String[] TIERS = {"", "I", "II", "III", "IV", "V"};

    /** The item name as in the RNG meter menu, without leading symbols like "◆". */
    String getItemName();

    /** The lowest boss tier that can drop it, 0 for {@code NONE}. */
    int getMinTier();

    /**
     * The wiki's estimate of the Slayer XP to fill the meter, only used until the RNG meter menu was read. It is a few
     * percent off for some drops, and the real value depends on the highest tier completed.
     */
    long getWikiRequiredXp();

    /** Whether this is the {@code NONE} constant. */
    default boolean isNone() {
        return this.getMinTier() == 0;
    }

    /** The config dropdown label, e.g. "Scythe Blade (Tier IV+)". */
    static @NotNull String label(@NotNull SlayerRngDrop drop) {
        return drop.isNone() ? drop.getItemName() : drop.getItemName() + " (Tier " + tier(drop.getMinTier()) + "+)";
    }

    static @NotNull String tier(int tier) {
        return tier > 0 && tier < TIERS.length ? TIERS[tier] : String.valueOf(tier);
    }

    /** Lower case letters and digits only, to compare names from the menu with {@link #getItemName()}. */
    static @NotNull String normalize(@NotNull String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

}
