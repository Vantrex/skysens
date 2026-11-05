package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.ItemFacts;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The held item by SkyBlock id (glob), reforge and enchantment. Items without ExtraAttributes never match. */
public final class HeldItemCondition implements ConditionType {

    @Override
    public String id() {
        return "item.held";
    }

    @Override
    public String displayName() {
        return "Held item";
    }

    @Override
    public String group() {
        return "Items";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                ParamSpec.of("id", "SkyBlock ID", "Item id, * is a wildcard (e.g. *_SWORD, HYPERION). Empty = any", ParamKind.TEXT, new JsonPrimitive("")),
                ParamSpec.of("reforge", "Reforge", "Reforge id, e.g. heroic. Empty = any", ParamKind.TEXT, new JsonPrimitive("")),
                ParamSpec.of("enchant", "Enchantment", "Enchantment id, e.g. ultimate_wise. Empty = any", ParamKind.TEXT, new JsonPrimitive("")),
                ParamSpec.of("minLevel", "Minimum level", "Minimum enchantment level", ParamKind.INT, new JsonPrimitive(1)));
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final ItemFacts items = ctx.items();
        final String id = Params.getString(params, "id", "").trim();
        final String reforge = Params.getString(params, "reforge", "").trim();
        final String enchant = Params.getString(params, "enchant", "").trim();
        if (items.held().isEmpty()) {
            return Tri.FALSE;
        }
        final boolean anyFilter = !id.isEmpty() || !reforge.isEmpty() || !enchant.isEmpty();
        final Optional<CompoundTag> extra = items.heldExtraAttributes();
        if (extra.isEmpty()) {
            return Tri.of(!anyFilter);
        }
        final CompoundTag ea = extra.get();
        if (!id.isEmpty() && !ItemFacts.matchesGlob(HypixelExtraAttributes.getString(ea, "id").orElse(null), id)) {
            return Tri.FALSE;
        }
        if (!reforge.isEmpty() && !reforge.equalsIgnoreCase(HypixelExtraAttributes.getString(ea, "modifier").orElse(""))) {
            return Tri.FALSE;
        }
        if (!enchant.isEmpty()) {
            final int level = ea.getCompound("enchantments").flatMap(enchants -> {
                for (String key : enchants.keySet()) {
                    if (key.equalsIgnoreCase(enchant)) {
                        return enchants.getInt(key);
                    }
                }
                return Optional.<Integer>empty();
            }).orElse(0);
            if (level <= 0 || level < Params.getInt(params, "minLevel", 1)) {
                return Tri.FALSE;
            }
        }
        return Tri.TRUE;
    }

    @Override
    public String describe(JsonObject params) {
        final List<String> parts = new ArrayList<>();
        final String id = Params.getString(params, "id", "").trim();
        if (!id.isEmpty()) {
            parts.add(id);
        }
        final String reforge = Params.getString(params, "reforge", "").trim();
        if (!reforge.isEmpty()) {
            parts.add("reforge " + reforge.toLowerCase(Locale.ROOT));
        }
        final String enchant = Params.getString(params, "enchant", "").trim();
        if (!enchant.isEmpty()) {
            parts.add(enchant + " " + Params.getInt(params, "minLevel", 1) + "+");
        }
        return "holding " + (parts.isEmpty() ? "an item" : String.join(", ", parts));
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getString(params, "id", "").isBlank() && Params.getString(params, "reforge", "").isBlank()
                && Params.getString(params, "enchant", "").isBlank()) {
            problems.add("No filter set, matches any held item");
        }
    }

}
