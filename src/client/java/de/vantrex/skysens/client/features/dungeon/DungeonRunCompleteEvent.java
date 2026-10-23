package de.vantrex.skysens.client.features.dungeon;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/** A dungeon run was completed, fired once per run by the {@link de.vantrex.skysens.client.features.dungeon.DungeonRunDetector}. */
@Data
public final class DungeonRunCompleteEvent implements SkysensEvent {
    /** "M7", "F5". */
    private final @NotNull String floor;
    private final long seconds;
    private final int score;
    /** "S+", "S", "A", ... */
    private final @NotNull String rank;
    /** Own deaths since entering the floor. */
    private final int deaths;
}
