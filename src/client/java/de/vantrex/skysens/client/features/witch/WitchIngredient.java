package de.vantrex.skysens.client.features.witch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The Year of the Witch ingredients, in the order of the Witch's Sack. Mob types, drop chances and uses are from the
 * Hypixel SkyBlock Wiki (Year of the Witch, Mob Types). The constant names are stored in the config and in
 * {@link WitchIngredientRepository}, don't rename them.
 */
public enum WitchIngredient {

    BRITTLE_POWDER("Brittle Powder", "Wither", "5%",
            List.of("Wither mobs, common in The Catacombs", "e.g. Crypt Dreadlords"),
            List.of(use("Withering Purée", 64), use("Giant Witch Cauldron", 64))),
    CONSOLE_PANEL("Console Panel", "Construct", "5%",
            List.of("Construct mobs, usually summoned by other", "mobs or players, e.g. Automatons"),
            List.of(use("Electronic Elixir", 64), use("Medium Witch Cauldron", 64))),
    CRUNCHY_BUG("Crunchy Bug", "Critter", "10%",
            List.of("Critter mobs on the Galatea islands, e.g. Coralots", "Need to be killed with Galatea tools"),
            List.of(use("Insect Casserole", 64))),
    DIRTY_FINGERNAILS("Dirty Fingernails", "Humanoid", "5%",
            List.of("Humanoid mobs, common in the Crystal Hollows", "e.g. Grunts"),
            List.of(use("Hopefully-not-human Hodgepodge", 64), use("Giant Witch Cauldron", 64))),
    FESTERING_FLY("Festering Fly", "Undead", "5%",
            List.of("Undead mobs, common in the SkyBlock Hub", "e.g. Zombies"),
            List.of(use("Mayfly Stew", 64), use("Witch Pet", 64), use("Witch's Talisman", 32), use("Witch's Sack", 16))),
    FROG_LEGS("Frog Legs", "Animal", "5%",
            List.of("Animal mobs, common on The Farming Islands", "e.g. Pigs"),
            List.of(use("Witch's Ring", 64), use("Frogspawn Chowder", 64), use("Pig Brain Mixin", 128))),
    FUNNY_BONE("Funny Bone", "Skeletal", "5%",
            List.of("Skeletal mobs, common in The Catacombs", "e.g. Skeletons"),
            List.of(use("Small Witch Cauldron", 64), use("Bone Broth", 64))),
    GELATINOUS_GLOOP("Gelatinous Gloop", "Cubic", "5%",
            List.of("Cubic mobs, common in the Deep Caverns", "e.g. Slimes"),
            List.of(use("Smelly Jelly", 64), use("Witch's Artifact", 64))),
    GLACIATED_BRAINS("Glaciated Brains", "Glacial", "5%",
            List.of("Glacial mobs, common in the Glacite Mineshafts", "e.g. Glacite Walkers",
                    "Can only be damaged with a pickaxe or drill"),
            List.of(use("Knockoff Glacite Gumbo", 64), use("Large Witch Cauldron", 64))),
    JAWBREAKER("Jawbreaker", "Subterranean", "5%",
            List.of("Subterranean mobs, common in the Dwarven Mines", "e.g. Treasure Hoarders"),
            List.of(use("Gemstone Goulash", 64), use("Medium Witch Cauldron", 64))),
    NEVER_MELT_ICE("Never-Melt Ice", "Frozen", "10%",
            List.of("Frozen mobs, common in Jerry's Workshop", "e.g. Frozen Steves",
                    "Also any time in the Icy biome of the Critter Safari"),
            List.of(use("Straight Ice Slushie", 64), use("Large Witch Cauldron", 64))),
    PIXIE_DUST("Pixie Dust", "Elusive", "50%",
            List.of("Elusive mobs: rare mobs that aren't seen often", "e.g. Diamond Goblins"),
            List.of(use("Effervescent Bouillon", 64), use("Giant Witch Cauldron", 64))),
    ROTTED_LEAF("Rotted Leaf", "Woodland", "5%",
            List.of("Woodland mobs, common in Galatea", "e.g. Chillshots",
                    "Can only be damaged with an axe or huntaxe"),
            List.of(use("Leafy Lentil Soup", 64))),
    RUFFLED_FEATHER("Ruffled Feather", "Airborne", "10%",
            List.of("Airborne mobs: mobs that can fly", "e.g. Bats"),
            List.of(use("Aerodynamic Potpourri", 64), use("Giant Witch Cauldron", 64))),
    SHIELD_DUST("Shield Dust", "Shielded", "10%",
            List.of("Shielded mobs: killed by a number of hits", "instead of damage, e.g. Scatha"),
            List.of(use("Dusty Bisque", 64), use("Giant Witch Cauldron", 64))),
    SMOLDERING_EMBERS("Smoldering Embers", "Infernal", "5%",
            List.of("Infernal mobs, common on the Crimson Isle", "e.g. Blazes"),
            List.of(use("Flaming Hot Chilli", 64), use("Witch's Artifact", 64), use("Large Witch Cauldron", 64))),
    SPELL_SOOT("Spell Soot", "Arcane", "5%",
            List.of("Arcane mobs, common on the Crimson Isle"),
            List.of(use("Spellweaver's Soup", 64), use("Witch's Artifact", 64))),
    SPIDER_EGG("Spider Egg", "Arthropod", "5%",
            List.of("Arthropod mobs, common in the Spider's Den", "e.g. Dasher Spiders"),
            List.of(use("Witch's Sack", 16), use("Spider Eye Soup", 64), use("Small Witch Cauldron", 64))),
    VOID_OOZE("Void Ooze", "Ender", "5%",
            List.of("Ender mobs, common in The End", "e.g. Zealots"),
            List.of(use("Void Porridge", 64), use("Witch's Ring", 64))),
    WARMED_FLAKES("Warmed Flakes", "Magmatic", "5%",
            List.of("Magmatic mobs, common on the Crimson Isle", "e.g. Fire Eels"),
            List.of(use("Cremated Cereal", 64), use("Large Witch Cauldron", 64))),
    WATERBOURNE_LICE("Waterbourne Lice", "Aquatic", "5%",
            List.of("Aquatic mobs, common when fishing", "e.g. Squids and sea creatures"),
            List.of(use("Blended Fish Mixin", 128), use("Waterlice Liquor", 64), use("Medium Witch Cauldron", 64)));

    /** What an amount of the ingredient is spent on, at Agnes or Alcina. */
    public record Use(@NotNull String item, int amount) {
    }

    private final String displayName;
    private final String mobType;
    private final String dropChance;
    private final List<String> where;
    private final List<Use> uses;
    private final int goal;

    WitchIngredient(String displayName, String mobType, String dropChance, List<String> where, List<Use> uses) {
        this.displayName = displayName;
        this.mobType = mobType;
        this.dropChance = dropChance;
        this.where = where;
        this.uses = uses;
        this.goal = uses.stream().mapToInt(Use::amount).sum();
    }

    private static Use use(String item, int amount) {
        return new Use(item, amount);
    }

    /** The item name as Hypixel shows it, e.g. "Never-Melt Ice". */
    public @NotNull String displayName() {
        return this.displayName;
    }

    public @NotNull String mobType() {
        return this.mobType;
    }

    public @NotNull String dropChance() {
        return this.dropChance;
    }

    /** Where the mobs that drop it are, a few short lines. */
    public @NotNull List<String> where() {
        return this.where;
    }

    public @NotNull List<Use> uses() {
        return this.uses;
    }

    /** How many are needed for everything it is used for (the wiki's "complete the event" amounts). */
    public int goal() {
        return this.goal;
    }

    /** The label in the config dropdown. */
    @Override
    public String toString() {
        return this.displayName;
    }

    /** The ingredient with this item name, ignoring case, or null. */
    public static @Nullable WitchIngredient byName(@NotNull String name) {
        final String key = name.trim().toLowerCase(Locale.ROOT);
        for (final WitchIngredient ingredient : values()) {
            if (ingredient.displayName.toLowerCase(Locale.ROOT).equals(key)) {
                return ingredient;
            }
        }
        return null;
    }

}
