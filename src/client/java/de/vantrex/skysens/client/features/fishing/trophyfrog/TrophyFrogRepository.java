package de.vantrex.skysens.client.features.fishing.trophyfrog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The player's Trophy Frog catches per Minecraft account and SkyBlock profile, saved as
 * {@code repositories/TrophyFrogRepository.json}. Don't rename the class or its fields, they are the file name and
 * the JSON keys.
 */
public class TrophyFrogRepository {

    /** Minecraft account UUID to its SkyBlock profiles. */
    public Map<String, Account> accounts = new HashMap<>();

    public @NotNull Account account(@NotNull String uuid) {
        return this.accounts.computeIfAbsent(uuid, key -> new Account());
    }

    public static class Account {
        /** SkyBlock profile id to its catches; keyed by the profile name ("Lemon") while the id isn't known. */
        public Map<String, Profile> profiles = new HashMap<>();

        /**
         * The profile by its id, or by its name while the id is unknown. Once the id is known, a profile stored under
         * the name is moved to the id.
         */
        public @NotNull Profile profile(@Nullable String id, @NotNull String name) {
            if (id == null) {
                return this.named(this.profiles.computeIfAbsent(name, key -> new Profile()), name);
            }
            Profile profile = this.profiles.get(id);
            if (profile == null) {
                final Profile byName = this.profiles.remove(name);
                profile = byName != null ? byName : new Profile();
                this.profiles.put(id, profile);
            }
            return this.named(profile, name);
        }

        private Profile named(Profile profile, String name) {
            profile.name = name;
            return profile;
        }
    }

    public static class Profile {
        /** The profile name ("Lemon") it was last seen with, to tell profiles apart in the file. */
        public @Nullable String name;
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
