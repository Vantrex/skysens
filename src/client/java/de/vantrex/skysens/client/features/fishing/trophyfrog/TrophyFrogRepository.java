package de.vantrex.skysens.client.features.fishing.trophyfrog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The player's Trophy Frog catches per SkyBlock profile, saved as {@code repositories/TrophyFrogRepository.json}.
 * Don't rename the class or its fields, they are the file name and the JSON keys.
 */
public class TrophyFrogRepository {

    /** The SkyBlock profile we were on last, e.g. "Lemon". Null until the tab list showed one. */
    public @Nullable String lastProfile;

    /** Profile name to its catches. */
    public Map<String, Profile> profiles = new HashMap<>();

    public @NotNull Profile profile(@NotNull String name) {
        return this.profiles.computeIfAbsent(name, key -> new Profile());
    }

    public static class Profile {
        /**
         * True once the Trophy Frogs menu was read on this profile. Until then the counts only hold the catches seen
         * in chat, so they are too low.
         */
        public boolean menuRead;
        /** Frog name ("Common Frog") to the amount caught per tier, in the order of the Trophy Frogs menu. */
        public Map<String, Map<TrophyFrogTier, Integer>> frogs = new LinkedHashMap<>();

        public @NotNull Map<TrophyFrogTier, Integer> frog(@NotNull String name) {
            return this.frogs.computeIfAbsent(name, key -> new EnumMap<>(TrophyFrogTier.class));
        }
    }

}
