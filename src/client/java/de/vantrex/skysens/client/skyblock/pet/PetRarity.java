package de.vantrex.skysens.client.skyblock.pet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The rarity of a pet, with the color Hypixel shows it in. {@link #UNKNOWN} if it couldn't be told. */
public enum PetRarity {
    COMMON(0xFFFFFF),
    UNCOMMON(0x55FF55),
    RARE(0x5555FF),
    EPIC(0xAA00AA),
    LEGENDARY(0xFFAA00),
    MYTHIC(0xFF55FF),
    DIVINE(0x55FFFF),
    UNKNOWN(-1);

    /** The RGB of the color code, -1 if none. */
    private final int rgb;

    PetRarity(int rgb) {
        this.rgb = rgb;
    }

    /** The rarity shown in this RGB color, {@link #UNKNOWN} for any other color (e.g. the gray of "[Lvl 100]"). */
    public static @NotNull PetRarity fromRgb(int rgb) {
        for (final PetRarity rarity : values()) {
            if (rarity.rgb >= 0 && rarity.rgb == (rgb & 0xFFFFFF)) {
                return rarity;
            }
        }
        return UNKNOWN;
    }

    /** The rarity by its name as in the {@code tier} field of the pet data ("LEGENDARY"), {@link #UNKNOWN} if not one. */
    public static @NotNull PetRarity fromName(@Nullable String name) {
        if (name != null) {
            for (final PetRarity rarity : values()) {
                if (rarity != UNKNOWN && rarity.name().equalsIgnoreCase(name.trim())) {
                    return rarity;
                }
            }
        }
        return UNKNOWN;
    }

}
