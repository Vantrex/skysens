package de.vantrex.skysens.common.dto.splits;

import de.vantrex.skysens.common.domain.dungeon.DungeonSplit;

import java.util.List;
import java.util.Map;

/**
 * Server-hosted replacement for the bundled {@code splits.json} (§5.1).
 *
 * <p>Keyed by floor token exactly as the bundled file is ("E", "F1", ...), so a
 * downloaded set is a drop-in substitute for the bundled one. Versioned as an
 * asset: the mod keeps the last good copy and only refetches on hash change.
 */
public record SplitDefinitionSetDto(
        String setId,
        String hash,
        Map<String, List<DungeonSplit>> splitsByFloor
) {
}
