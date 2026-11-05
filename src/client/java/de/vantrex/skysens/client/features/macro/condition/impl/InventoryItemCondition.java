package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.List;

/** The inventory (main, hotbar, offhand) holds at least N items with a matching SkyBlock id. */
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
        return List.of(
                ParamSpec.of("id", "SkyBlock ID", "Item id, * is a wildcard (e.g. ENDER_PEARL, *_SWORD)", ParamKind.TEXT, new JsonPrimitive("")),
                ParamSpec.of("minCount", "Minimum count", "At least this many items", ParamKind.INT, new JsonPrimitive(1)));
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final String id = Params.getString(params, "id", "").trim();
        if (id.isEmpty()) {
            return Tri.FALSE;
        }
        return Tri.of(ctx.items().countInventory(id) >= Math.max(1, Params.getInt(params, "minCount", 1)));
    }

    @Override
    public String describe(JsonObject params) {
        return "inventory has " + Math.max(1, Params.getInt(params, "minCount", 1)) + "x " + Params.getString(params, "id", "?");
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getString(params, "id", "").isBlank()) {
            problems.add("No item id set, never matches");
        }
    }

}
