package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonClassTracker;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;

/** Dungeon state. The only macro class that reads the dungeon tracker (floor and class conditions come later). */
public final class DungeonFacts {

    /** Whether a dungeon run is on; UNKNOWN while the location isn't known. */
    public Tri inRun(LocationFacts location) {
        final SkyblockLocationEnum island = location.island();
        if (island == null) {
            return Tri.UNKNOWN;
        }
        return Tri.of(island == SkyblockLocationEnum.CATACOMBS || DungeonClassTracker.getInstance().isInDungeon());
    }

}
