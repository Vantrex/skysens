package de.vantrex.skysens.client.features.fishing.trophyfrog;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The four tiers of a Trophy Frog, lowest first. The constant names are stored in {@link TrophyFrogRepository}, don't
 * rename them.
 */
public enum TrophyFrogTier {

    BRONZE("§8"),
    SILVER("§7"),
    GOLD("§6"),
    DIAMOND("§b");

    private final String colorCode;

    TrophyFrogTier(String colorCode) {
        this.colorCode = colorCode;
    }

    public String colorCode() {
        return this.colorCode;
    }

    /** "Gold", "GOLD" or "gold" as a tier, null for anything else. */
    public static @Nullable TrophyFrogTier byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
