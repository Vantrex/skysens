package de.vantrex.skysens.client.features.macro.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Params;

/** One rule {@code {id, type, negate, params}}, a view over its JSON. Applies negation: not UNKNOWN stays UNKNOWN. */
public final class ConditionRule {

    private final JsonObject json;

    public ConditionRule(JsonObject json) {
        this.json = json;
    }

    public String id() {
        return Params.getString(this.json, "id", "");
    }

    public String type() {
        return Params.getString(this.json, "type", "");
    }

    public boolean negate() {
        return Params.getBool(this.json, "negate", false);
    }

    public JsonObject params() {
        final JsonElement params = this.json.get("params");
        return params != null && params.isJsonObject() ? params.getAsJsonObject() : new JsonObject();
    }

    public Tri evaluate(MacroContext ctx, ReasonSink reasons) {
        final ConditionType type = ConditionTypes.get(this.type());
        if (type == null) {
            reasons.unknown("unknown condition type \"" + this.type() + "\"");
            return Tri.UNKNOWN;
        }
        final Tri result = type.test(this.params(), ctx, reasons);
        return this.negate() ? result.not() : result;
    }

    public String describe() {
        final ConditionType type = ConditionTypes.get(this.type());
        final String text = type == null ? "unknown condition \"" + this.type() + "\"" : type.describe(this.params());
        return this.negate() ? "not " + text : text;
    }

}
