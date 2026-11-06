package de.vantrex.skysens.client.skyblock.dungeon;

import java.util.HashMap;
import java.util.Map;

/**
 * The last known own dungeon class per SkyBlock profile, saved as {@code repositories/DungeonClassRepository.json}.
 * Don't rename the class or its fields, they are the file name and the JSON keys.
 */
public class DungeonClassRepository {

    /** Profile name to {@link DungeonClass} constant name. */
    public Map<String, String> selectedClasses = new HashMap<>();
    /** Profile name + ":" + {@link DungeonClass} constant name to the last seen class level. */
    public Map<String, Integer> classLevels = new HashMap<>();
}
