package de.vantrex.skysens.common.dto.splits;

import de.vantrex.skysens.common.domain.dungeon.DungeonFloorEnum;
import de.vantrex.skysens.common.domain.dungeon.DungeonModeEnum;

import java.time.Instant;
import java.util.Map;

/**
 * Read model for a player's personal bests (§5.1).
 *
 * @param bestSplitDurationsMillis the best time achieved for each split
 *                                 independently — a "sum of best", not one run
 */
public record PersonalBestDto(
        DungeonFloorEnum floor,
        DungeonModeEnum mode,
        long bestTotalDurationMillis,
        Instant achievedAt,
        Map<String, Long> bestSplitDurationsMillis
) {
}
