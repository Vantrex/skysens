package de.vantrex.skysens.client.skyblock.dungeon;

import org.jetbrains.annotations.Nullable;

/**
 * A member of the dungeon party, from the dungeon tab list.
 *
 * @param dungeonClass the class, null while unknown
 * @param level        the class level, 0 while unknown
 * @param alive        false while the tab list shows the member as dead
 */
public record DungeonPartyMember(String name, @Nullable DungeonClass dungeonClass, int level, boolean alive) {
}
