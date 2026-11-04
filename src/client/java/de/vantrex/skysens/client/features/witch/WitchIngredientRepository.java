package de.vantrex.skysens.client.features.witch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The Year of the Witch ingredients in the player's Witch's Sack, per Minecraft account and SkyBlock profile, saved as
 * {@code repositories/WitchIngredientRepository.json}. Don't rename the class or its fields, they are the file name
 * and the JSON keys.
 */
public class WitchIngredientRepository {

    /** Minecraft account UUID to its SkyBlock profiles. */
    public Map<String, Account> accounts = new HashMap<>();

    public @NotNull Account account(@NotNull String uuid) {
        return this.accounts.computeIfAbsent(uuid, key -> new Account());
    }

    public static class Account {
        /** SkyBlock profile id to its ingredients; keyed by the profile name ("Lemon") while the id isn't known. */
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
         * True once the Witch's Sack was opened on this profile. Until then the amounts only hold what the sack
         * messages added and removed, so they can be off.
         */
        public boolean sackRead;
        /** Ingredient to the amount stored in the Witch's Sack. Missing means none. */
        public Map<WitchIngredient, Integer> stored = new EnumMap<>(WitchIngredient.class);

        public int amount(@NotNull WitchIngredient ingredient) {
            final Integer amount = this.stored.get(ingredient);
            return amount == null ? 0 : amount;
        }
    }

}
