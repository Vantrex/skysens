package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.PetFacts;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.skyblock.pet.Pet;
import de.vantrex.skysens.client.skyblock.pet.PetRarity;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The pet type / rarity / level filter shared by the active pet, held item and inventory item conditions. Params:
 * {@code petType} (free text, several types separated by commas), {@code petRarities} (names) and {@code petMinLevel}.
 * Rarity is stored as the enum name, type as the Hypixel pet type id.
 */
public final class PetFilter {

    public static final String TYPE = "petType";
    public static final String RARITIES = "petRarities";
    public static final String MIN_LEVEL = "petMinLevel";

    private PetFilter() {
    }

    public static List<ParamSpec> specs() {
        return List.of(
                new ParamSpec(TYPE, "Pet type", "Pet type id, e.g. GOLDEN_DRAGON (type it or pick one). Empty = any", ParamKind.TEXT_CHOICES,
                        new JsonPrimitive(""), () -> PetFacts.typeChoices(heldPet()), null),
                new ParamSpec(RARITIES, "Pet rarity", "Matches when the pet has any of these rarities. None = any", ParamKind.ENUM_MULTI,
                        new JsonArray(), PetFacts::rarityChoices, null),
                ParamSpec.of(MIN_LEVEL, "Minimum pet level", "0 = any level", ParamKind.INT, new JsonPrimitive(0)));
    }

    private static Optional<Pet> heldPet() {
        final Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? Optional.empty() : PetFacts.fromStack(mc.player.getMainHandItem());
    }

    public static List<String> types(JsonObject params) {
        final List<String> out = new ArrayList<>();
        for (String part : Params.getString(params, TYPE, "").split(",")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    public static boolean isSet(JsonObject params) {
        return !types(params).isEmpty() || !Params.getStringList(params, RARITIES).isEmpty() || Params.getInt(params, MIN_LEVEL, 0) > 0;
    }

    /** FALSE as soon as a known value does not match, else UNKNOWN if a needed value is missing, else TRUE. */
    public static Tri test(JsonObject params, Pet pet, ReasonSink reasons) {
        final List<String> types = types(params);
        if (!types.isEmpty() && types.stream().noneMatch(t -> t.equalsIgnoreCase(pet.type()))) {
            return Tri.FALSE;
        }
        boolean unknown = false;
        final List<String> rarities = Params.getStringList(params, RARITIES);
        if (!rarities.isEmpty()) {
            if (pet.rarity() == PetRarity.UNKNOWN) {
                reasons.unknown("pet rarity not known");
                unknown = true;
            } else if (!rarities.contains(pet.rarity().name())) {
                return Tri.FALSE;
            }
        }
        final int min = Params.getInt(params, MIN_LEVEL, 0);
        if (min > 0) {
            if (pet.level() <= 0) {
                reasons.unknown("pet level not known");
                unknown = true;
            } else if (pet.level() < min) {
                return Tri.FALSE;
            }
        }
        return unknown ? Tri.UNKNOWN : Tri.TRUE;
    }

    /**
     * Tests an item against the filter. An item without {@code petInfo} is no pet: FALSE. A {@code petInfo} that
     * cannot be parsed gives UNKNOWN.
     */
    public static Tri testStack(JsonObject params, ItemStack stack, ReasonSink reasons) {
        final boolean hasPetInfo = HypixelExtraAttributes.getExtraAttributes(stack).map(ea -> ea.contains("petInfo")).orElse(false);
        if (!hasPetInfo) {
            return Tri.FALSE;
        }
        final Optional<Pet> pet = PetFacts.fromStack(stack);
        if (pet.isEmpty()) {
            reasons.unknown("pet data of the item could not be read");
            return Tri.UNKNOWN;
        }
        return test(params, pet.get(), reasons);
    }

    public static String describe(JsonObject params) {
        final List<String> parts = new ArrayList<>();
        final List<String> types = types(params);
        if (!types.isEmpty()) {
            parts.add(String.join("/", types));
        }
        final List<String> rarities = Params.getStringList(params, RARITIES);
        if (!rarities.isEmpty()) {
            final List<String> names = new ArrayList<>();
            rarities.forEach(r -> names.add(r.toLowerCase(Locale.ROOT)));
            parts.add(String.join("/", names));
        }
        final int min = Params.getInt(params, MIN_LEVEL, 0);
        if (min > 0) {
            parts.add("level " + min + "+");
        }
        return String.join(" ", parts);
    }

    public static void validate(JsonObject params, List<String> problems) {
        for (String rarity : Params.getStringList(params, RARITIES)) {
            if (!PetFacts.isRarity(rarity)) {
                problems.add("Unknown pet rarity " + rarity);
            }
        }
    }

}
