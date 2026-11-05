package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonClass;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/** Fired by {@code DungeonClassTracker} when the player's own selected dungeon class changes; null while unknown. */
@Data
public final class DungeonClassChangeEvent implements SkysensEvent {

    private final @Nullable DungeonClass oldClass;
    private final @Nullable DungeonClass newClass;

}
