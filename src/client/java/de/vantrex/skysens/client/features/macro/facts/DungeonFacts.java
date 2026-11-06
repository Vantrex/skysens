package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonClass;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonClassTracker;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonPartyMember;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Dungeon state: the run, the floor, the own class and the party's classes. The macro class that reads the dungeon tracker. */
public final class DungeonFacts {

    private final DungeonClassTracker tracker = DungeonClassTracker.getInstance();

    /** Whether a dungeon run is on; UNKNOWN while the location is not known. */
    public Tri inRun(LocationFacts location) {
        final SkyblockLocationEnum island = location.island();
        if (island == null) {
            return Tri.UNKNOWN;
        }
        return Tri.of(island == SkyblockLocationEnum.CATACOMBS || this.tracker.isInDungeon());
    }

    /** "F7" / "M7", null outside a run or while not read yet. */
    public @Nullable String floor() {
        return this.tracker.getFloor();
    }

    public @Nullable DungeonClass ownClass() {
        return this.tracker.getSelectedClass();
    }

    /** 0 = unknown. */
    public int ownLevel() {
        return this.tracker.getSelectedClassLevel();
    }

    /** The party of the run including the player; empty outside a run or before the tab list was read. */
    public List<DungeonPartyMember> members() {
        return this.tracker.getPartyMembers();
    }

    public @Nullable DungeonPartyMember member(String name) {
        return this.tracker.getPartyMember(name);
    }

    public static List<Choice> classChoices() {
        final List<Choice> out = new ArrayList<>();
        for (DungeonClass c : DungeonClass.values()) {
            out.add(new Choice(c.name(), c.getDisplayName()));
        }
        return out;
    }

    public static boolean isClass(String name) {
        for (DungeonClass c : DungeonClass.values()) {
            if (c.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static String classDisplayName(String name) {
        for (DungeonClass c : DungeonClass.values()) {
            if (c.name().equals(name)) {
                return c.getDisplayName();
            }
        }
        return name;
    }

    /** F1-F7 and M1-M7. The Entrance is not offered: the tracker only reads "The Catacombs (F7)" style floors. */
    public static List<Choice> floorChoices() {
        final List<Choice> out = new ArrayList<>();
        for (String prefix : new String[]{"F", "M"}) {
            for (int i = 1; i <= 7; i++) {
                out.add(new Choice(prefix + i, prefix + i));
            }
        }
        return out;
    }

}
