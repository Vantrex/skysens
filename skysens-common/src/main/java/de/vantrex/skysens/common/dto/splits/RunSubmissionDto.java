package de.vantrex.skysens.common.dto.splits;

import de.vantrex.skysens.common.domain.dungeon.DungeonFloorEnum;
import de.vantrex.skysens.common.domain.dungeon.DungeonModeEnum;

import java.time.Instant;
import java.util.Map;

/**
 * One completed dungeon run pushed by the mod (§5.1), write-only.
 *
 * <p>Carries no UUID: identity comes from the bearer token, never from the body.
 *
 * @param splitDurationsMillis split name -> elapsed millis, using the names from
 *                             the {@link SplitDefinitionSetDto} identified by
 *                             {@code splitSetHash}
 */
public record RunSubmissionDto(
        DungeonFloorEnum floor,
        DungeonModeEnum mode,
        Instant startedAt,
        long totalDurationMillis,
        String splitSetHash,
        Map<String, Long> splitDurationsMillis
) {
}
