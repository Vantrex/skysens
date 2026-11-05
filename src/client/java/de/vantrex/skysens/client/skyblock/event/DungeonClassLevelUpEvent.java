package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonClass;
import lombok.Data;

/** Fired when the player's own level in their selected class rose (seen on the dungeon tab list or the party finder). */
@Data
public final class DungeonClassLevelUpEvent implements SkysensEvent {

    private final DungeonClass dungeonClass;
    private final int oldLevel;
    private final int newLevel;

}
