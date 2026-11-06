package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.skyblock.pet.Pet;
import de.vantrex.skysens.client.skyblock.pet.PetInfoParser;
import de.vantrex.skysens.client.skyblock.pet.PetRarity;
import de.vantrex.skysens.client.skyblock.pet.PetTracker;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** The active pet and pet items. With {@link DungeonFacts} the macro class that touches the pet package. */
public final class PetFacts {

    /** Pet types suggested even before the /pets menu was opened. */
    private static final List<String> COMMON_PETS = List.of("GOLDEN_DRAGON", "ENDER_DRAGON", "BLUE_WHALE", "ELEPHANT", "TIGER",
            "LION", "MONKEY", "GIRAFFE", "BEE", "BAT", "WITHER_SKELETON", "SPIRIT", "ROCK", "DOLPHIN", "FLYING_FISH", "SQUID",
            "GUARDIAN", "ENDERMAN", "BLAZE", "JERRY", "PARROT", "TURTLE", "SHEEP", "SILVERFISH", "WOLF", "PHOENIX", "GRIFFIN",
            "BLACK_CAT", "JELLYFISH", "MEGALODON", "ARMADILLO", "MITHRIL_GOLEM", "SCATHA", "SNOWMAN", "REINDEER", "HOUND",
            "MAGMA_CUBE", "SKELETON_HORSE", "ENDERMITE", "AMMONITE");

    private final PetTracker tracker = PetTracker.getInstance();

    /** Whether the pet state is known (the per-profile pet was restored): TRUE or UNKNOWN, never FALSE. */
    public Tri known() {
        return this.tracker.isStateKnown() ? Tri.TRUE : Tri.UNKNOWN;
    }

    /** The active pet; only meaningful when {@link #known()} is TRUE. */
    public Optional<Pet> active() {
        return this.tracker.getActivePet();
    }

    public static Optional<Pet> fromStack(ItemStack stack) {
        return PetInfoParser.fromStack(stack);
    }

    /** Pet rarities the editor offers (no UNKNOWN). */
    public static List<Choice> rarityChoices() {
        final List<Choice> out = new ArrayList<>();
        for (PetRarity rarity : PetRarity.values()) {
            if (rarity != PetRarity.UNKNOWN) {
                out.add(new Choice(rarity.name(), titleCase(rarity.name())));
            }
        }
        return out;
    }

    public static boolean isRarity(String name) {
        return PetRarity.fromName(name) != PetRarity.UNKNOWN;
    }

    /** Pet type suggestions: the active and held pet, the pets seen in the /pets menu and some common ones. */
    public static List<Choice> typeChoices(Optional<Pet> held) {
        final Map<String, String> out = new LinkedHashMap<>();
        final PetTracker tracker = PetTracker.getInstance();
        tracker.getActivePet().ifPresent(p -> out.putIfAbsent(p.type(), p.name()));
        held.ifPresent(p -> out.putIfAbsent(p.type(), p.name()));
        for (Pet pet : tracker.getKnownPets()) {
            out.putIfAbsent(pet.type(), pet.name());
        }
        for (String type : COMMON_PETS) {
            out.putIfAbsent(type, titleCase(type));
        }
        final List<Choice> choices = new ArrayList<>();
        out.forEach((type, name) -> choices.add(new Choice(type, name)));
        return choices;
    }

    public static String titleCase(String id) {
        final StringBuilder sb = new StringBuilder();
        for (String part : id.toLowerCase(Locale.ROOT).split("_")) {
            if (!part.isEmpty()) {
                sb.append(sb.length() == 0 ? "" : " ").append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

}
