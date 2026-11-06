package de.vantrex.skysens.client.skyblock.pet;

import com.google.gson.annotations.Expose;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The active pet last seen per SkyBlock profile, saved to config/skysens/repositories/ActivePetRepository.json, so it
 * is known right after joining until the tab list confirms it. Don't rename the class or its fields, they are the
 * file name and the JSON keys.
 */
@NoArgsConstructor
public class ActivePetRepository {

    /** Profile name ("Lemon") to its active pet; a profile without an active pet has no entry. */
    @Expose
    public Map<String, PetData> activePets = new HashMap<>();

    public synchronized @Nullable Pet get(@NotNull String profile) {
        final PetData data = this.activePets.get(profile);
        return data == null || data.type == null ? null : data.toPet();
    }

    public synchronized void set(@NotNull String profile, @Nullable Pet pet) {
        if (pet == null) {
            this.activePets.remove(profile);
        } else {
            this.activePets.put(profile, PetData.of(pet));
        }
    }

    @NoArgsConstructor
    public static class PetData {
        @Expose
        public String type;
        @Expose
        public String name;
        @Expose
        public String rarity;
        @Expose
        public int level;
        @Expose
        public String heldItem;
        @Expose
        public String skin;
        @Expose
        public String uuid;
        @Expose
        public int candyUsed;

        static PetData of(Pet pet) {
            final PetData data = new PetData();
            data.type = pet.type();
            data.name = pet.name();
            data.rarity = pet.rarity().name();
            data.level = pet.level();
            data.heldItem = pet.heldItem();
            data.skin = pet.skin();
            data.uuid = pet.uuid();
            data.candyUsed = pet.candyUsed();
            return data;
        }

        Pet toPet() {
            return new Pet(this.type, this.name == null ? this.type : this.name, PetRarity.fromName(this.rarity),
                    this.level, this.heldItem, this.skin, this.uuid, this.candyUsed);
        }
    }

}
