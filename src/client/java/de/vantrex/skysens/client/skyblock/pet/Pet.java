package de.vantrex.skysens.client.skyblock.pet;

import de.vantrex.skysens.client.skyblock.item.ItemIdUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A pet as far as it is known.
 *
 * @param type      the pet type id, e.g. "GOLDEN_DRAGON"
 * @param name      the display name, e.g. "Golden Dragon"
 * @param rarity    the rarity, {@link PetRarity#UNKNOWN} if not known
 * @param level     the level, 0 if not known
 * @param heldItem  the id of the held pet item, e.g. "PET_ITEM_TIER_BOOST", null if none or not known
 * @param skin      the skin id, null if none or not known
 * @param uuid      the pet's uuid, only known for pets read from an item
 * @param candyUsed how many pet candies were used, 0 if none or not known
 */
public record Pet(@NotNull String type, @NotNull String name, @NotNull PetRarity rarity, int level,
                  @Nullable String heldItem, @Nullable String skin, @Nullable String uuid, int candyUsed) {

    /** A pet known by its display name only. */
    public static @NotNull Pet ofName(@NotNull String name, @NotNull PetRarity rarity, int level) {
        return new Pet(ItemIdUtil.toId(name), name, rarity, level, null, null, null, 0);
    }

    public @NotNull Pet withLevel(int level) {
        return new Pet(this.type, this.name, this.rarity, level, this.heldItem, this.skin, this.uuid, this.candyUsed);
    }

    public @NotNull Pet withRarity(@NotNull PetRarity rarity) {
        return new Pet(this.type, this.name, rarity, this.level, this.heldItem, this.skin, this.uuid, this.candyUsed);
    }

    /** "[Lvl 100] Golden Dragon" */
    public @NotNull String displayName() {
        return this.level > 0 ? "[Lvl " + this.level + "] " + this.name : this.name;
    }

}
