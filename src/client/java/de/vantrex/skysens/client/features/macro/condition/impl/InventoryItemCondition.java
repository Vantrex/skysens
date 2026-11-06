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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The inventory (main, hotbar, offhand) holds at least N items with a matching SkyBlock id. Never UNKNOWN. Pets are not
 * items here: use the Active pet condition.
 */
public final class InventoryItemCondition implements ConditionType {

    @Override
    public String id() {
        return "item.inventory";
    }

    @Override
    public String displayName() {
        return "Inventory item";
    }

    @Override
    public String group() {
        return "Items";
    }

    @Override
    public List<ParamSpec> params() {
        final List<ParamSpec> out = new ArrayList<>(List.of(
                ParamSpec.of("id", "SkyBlock ID", "Item id, * is a wildcard (e.g. ENDER_PEARL, *_SWORD)", ParamKind.TEXT, new JsonPrimitive("")),
                ParamSpec.of("minCount", "Minimum count", "At least this many items", ParamKind.INT, new JsonPrimitive(1))));
        return out;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final String id = Params.getString(params, "id", "").trim();
        if (id.isEmpty()) {
            return Tri.FALSE;
        }
        final int min = Math.max(1, Params.getInt(params, "minCount", 1));
        int count = 0;
        for (ItemStack stack : ctx.items().inventoryStacks()) {
            final String stackId = HypixelExtraAttributes.getExtraAttributes(stack)
                    .flatMap(ea -> HypixelExtraAttributes.getString(ea, "id")).orElse(null);
            if (ItemFacts.matchesGlob(stackId, id)) {
                count += stack.getCount();
            }
        }
        return Tri.of(count >= min);
    }

    @Override
    public String describe(JsonObject params) {
        final String id = Params.getString(params, "id", "").trim();
        return "inventory has " + Math.max(1, Params.getInt(params, "minCount", 1)) + "x " + (id.isEmpty() ? "?" : id);
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getString(params, "id", "").isBlank()) {
            problems.add("No item id set, never matches");
        }
    }

}
