package de.vantrex.skysens.client.skyblock.dungeon;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** The five dungeon classes. */
@Getter
public enum DungeonClass {
    HEALER("Healer"),
    MAGE("Mage"),
    BERSERK("Berserk"),
    ARCHER("Archer"),
    TANK("Tank");

    private final String displayName;

    DungeonClass(String displayName) {
        this.displayName = displayName;
    }

    /** Parses "Mage", "mage", "Berserker" or "Mage Class"; null if it is no class. */
    public static @Nullable DungeonClass fromDisplayName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        final String normalized = name.trim().toLowerCase(Locale.ROOT).replaceAll(" class$", "");
        for (final DungeonClass value : values()) {
            if (value.displayName.toLowerCase(Locale.ROOT).equals(normalized)) {
                return value;
            }
        }
        if (normalized.equals("berserker")) {
            return BERSERK;
        }
        for (final DungeonClass value : values()) {
            if (value.name().equalsIgnoreCase(normalized)) {
                return value;
            }
        }
        return null;
    }
}
