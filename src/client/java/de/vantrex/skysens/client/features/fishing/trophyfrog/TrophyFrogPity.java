package de.vantrex.skysens.client.features.fishing.trophyfrog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Trophy Frog pity, from the wiki (Trophy Frogs, "Obtaining"): while a frog was never caught in Gold or Diamond, its
 * 101st catch is guaranteed Gold and its 601st Diamond. The Exploding Frog and the Puddle Jumper are guaranteed on the
 * 51st and 301st catch instead. The counter is the frog's total catches over all tiers.
 */
final class TrophyFrogPity {

    private static final int GOLD = 100;
    private static final int DIAMOND = 600;
    private static final int HALF_GOLD = 50;
    private static final int HALF_DIAMOND = 300;
    /** Frogs with half the usual pity, by their name in the Trophy Frogs menu. */
    private static final Set<String> HALF_PITY = Set.of("Exploding Frog", "Puddle Jumper");

    private TrophyFrogPity() {
    }

    /** Catches without the tier after which the next catch is that tier, null for Bronze and Silver (no pity). */
    static @Nullable Integer threshold(@NotNull String frog, @NotNull TrophyFrogTier tier) {
        final boolean half = HALF_PITY.contains(frog);
        return switch (tier) {
            case GOLD -> half ? HALF_GOLD : GOLD;
            case DIAMOND -> half ? HALF_DIAMOND : DIAMOND;
            default -> null;
        };
    }

    /**
     * Catches of the frog until the guaranteed one, including it (1 means the next catch). Null if the tier has no
     * pity or was already caught, then pity doesn't apply anymore.
     */
    static @Nullable Integer catchesLeft(@NotNull String frog, @NotNull TrophyFrogTier tier,
                                         @NotNull Map<TrophyFrogTier, Integer> counts) {
        final Integer threshold = threshold(frog, tier);
        if (threshold == null || counts.getOrDefault(tier, 0) > 0) {
            return null;
        }
        // too low counts (menu not read) can push the total past the threshold, the next catch is the earliest then
        return Math.max(1, threshold + 1 - TrophyFrogService.total(counts));
    }

}
