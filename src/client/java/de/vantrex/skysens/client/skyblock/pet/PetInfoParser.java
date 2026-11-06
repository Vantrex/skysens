package de.vantrex.skysens.client.skyblock.pet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import de.vantrex.skysens.client.util.ChatUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds {@link Pet}s from pet items (the {@code petInfo} of their ExtraAttributes) and from display strings like
 * "[Lvl 100] Golden Dragon". Pure parsing, usable from any thread.
 */
public final class PetInfoParser {

    private static final Pattern DISPLAY = Pattern.compile("^\\[Lvl (\\d+)] (.+)$");

    private PetInfoParser() {
    }

    /** The pet an item is, empty if it has no {@code petInfo}. Works for pet items in any inventory or the /pets menu. */
    public static @NotNull Optional<Pet> fromStack(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        final JsonObject info = petInfo(stack);
        final String type = info == null ? null : string(info, "type");
        if (type == null) {
            return Optional.empty();
        }
        final Component hoverName = stack.getHoverName();
        final Matcher display = DISPLAY.matcher(clean(hoverName.getString()));
        final boolean named = display.matches();
        final int level = named ? Integer.parseInt(display.group(1)) : 0;
        final String name = named ? cleanName(display.group(2)) : titleCase(type);
        PetRarity rarity = named ? rarityOf(hoverName, name) : PetRarity.UNKNOWN;
        if (rarity == PetRarity.UNKNOWN) {
            rarity = PetRarity.fromName(string(info, "tier"));
        }
        int candy = 0;
        if (info.has("candyUsed") && info.get("candyUsed").isJsonPrimitive()) {
            try {
                candy = info.get("candyUsed").getAsInt();
            } catch (RuntimeException ignored) {
                // not a number
            }
        }
        return Optional.of(new Pet(type, name, rarity, level, blankToNull(string(info, "heldItem")),
                blankToNull(string(info, "skin")), blankToNull(string(info, "uuid")), candy));
    }

    /** Whether the item says that it is the active pet: {@code petInfo.active}, or "Click to despawn!" in the lore. */
    public static boolean isMarkedActive(@NotNull ItemStack stack) {
        final JsonObject info = petInfo(stack);
        if (info != null && "true".equalsIgnoreCase(string(info, "active"))) {
            return true;
        }
        final ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (final Component line : lore.lines()) {
                if (clean(line.getString()).startsWith("Click to despawn")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A pet from "[Lvl 100] Golden Dragon" (formatting codes are ignored), empty if the text isn't one. */
    public static @NotNull Optional<Pet> fromDisplay(@NotNull String text, @NotNull PetRarity rarity) {
        final Matcher matcher = DISPLAY.matcher(clean(text));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        final String name = cleanName(matcher.group(2));
        return name.isEmpty() ? Optional.empty()
                : Optional.of(Pet.ofName(name, rarity, Integer.parseInt(matcher.group(1))));
    }

    /** Like {@link #fromDisplay(String, PetRarity)}, with the rarity taken from the color of the name in the component. */
    public static @NotNull Optional<Pet> fromDisplay(@NotNull Component component) {
        final Matcher matcher = DISPLAY.matcher(clean(component.getString()));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return fromDisplay(component.getString(), rarityOf(component, cleanName(matcher.group(2))));
    }

    /** The rarity from the color of the segment of the component that has the name; unknown if there is no such color. */
    public static @NotNull PetRarity rarityOf(@NotNull Component component, @NotNull String name) {
        final List<Integer> colors = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        component.visit((style, text) -> {
            colors.add(style.getColor() == null ? -1 : style.getColor().getValue());
            texts.add(text);
            return Optional.empty();
        }, Style.EMPTY);
        for (int i = 0; i < texts.size(); i++) {
            if (colors.get(i) >= 0 && !name.isEmpty() && texts.get(i).contains(name)) {
                final PetRarity rarity = PetRarity.fromRgb(colors.get(i));
                if (rarity != PetRarity.UNKNOWN) {
                    return rarity;
                }
            }
        }
        // the name may share a segment with something else or be split: take the last rarity colored segment
        for (int i = texts.size() - 1; i >= 0; i--) {
            if (colors.get(i) >= 0 && !texts.get(i).isBlank()) {
                final PetRarity rarity = PetRarity.fromRgb(colors.get(i));
                if (rarity != PetRarity.UNKNOWN) {
                    return rarity;
                }
            }
        }
        return PetRarity.UNKNOWN;
    }

    /** The name without skin markers or other symbols and surplus spaces. */
    public static @NotNull String cleanName(@NotNull String name) {
        return name.replaceAll("[^\\p{L}\\p{N}' .-]+", "").replaceAll("\\s+", " ").trim();
    }

    /** The text without formatting codes, with non-breaking and repeated spaces collapsed. */
    public static @NotNull String clean(@NotNull String text) {
        return ChatUtil.strip(text).replace(' ', ' ').replaceAll("\\s+", " ");
    }

    private static @Nullable JsonObject petInfo(@NotNull ItemStack stack) {
        final Optional<CompoundTag> extra = HypixelExtraAttributes.getExtraAttributes(stack);
        if (extra.isEmpty() || !extra.get().contains("petInfo")) {
            return null;
        }
        final CompoundTag tag = extra.get();
        final Optional<String> json = tag.getString("petInfo");
        if (json.isPresent()) {
            try {
                final JsonElement element = JsonParser.parseString(json.get());
                return element.isJsonObject() ? element.getAsJsonObject() : null;
            } catch (RuntimeException e) {
                return null;
            }
        }
        // in case the data is a compound instead of a JSON string
        final Optional<CompoundTag> compound = tag.getCompound("petInfo");
        if (compound.isEmpty()) {
            return null;
        }
        final JsonObject object = new JsonObject();
        for (final String key : compound.get().keySet()) {
            final Tag value = compound.get().get(key);
            if (value != null) {
                object.addProperty(key, value.toString().replaceAll("^\"|\"$", ""));
            }
        }
        return object;
    }

    private static @Nullable String string(JsonObject object, String key) {
        final JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static @Nullable String blankToNull(@Nullable String text) {
        return text == null || text.isBlank() || text.equals("null") ? null : text;
    }

    private static String titleCase(String type) {
        final StringBuilder out = new StringBuilder();
        for (final String part : type.toLowerCase(Locale.ROOT).split("_")) {
            if (!part.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return out.toString();
    }

}
