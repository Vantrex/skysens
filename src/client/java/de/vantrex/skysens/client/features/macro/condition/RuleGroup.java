package de.vantrex.skysens.client.features.macro.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.ArrayList;
import java.util.List;

/**
 * Conditions combined with AND or OR: {@code {id, combinator, conditions: [...]}}. The If node's params are one group,
 * a Switch node holds one group per output.
 * <p>
 * AND: any FALSE is FALSE, else any UNKNOWN is UNKNOWN, else TRUE. OR: any TRUE is TRUE, else any UNKNOWN is UNKNOWN,
 * else FALSE. An empty group is TRUE.
 */
public final class RuleGroup {

    public enum Combinator { AND, OR }

    private final JsonObject json;

    public RuleGroup(JsonObject json) {
        this.json = json;
    }

    public String id() {
        return Params.getString(this.json, "id", "");
    }

    /** Optional label, used for the Switch output. */
    public String name() {
        return Params.getString(this.json, "name", "");
    }

    public Combinator combinator() {
        return Params.getEnum(this.json, "combinator", Combinator.class, Combinator.AND);
    }

    public List<ConditionRule> rules() {
        final List<ConditionRule> out = new ArrayList<>();
        final JsonElement conditions = this.json.get("conditions");
        if (conditions != null && conditions.isJsonArray()) {
            for (JsonElement e : conditions.getAsJsonArray()) {
                if (e.isJsonObject()) {
                    out.add(new ConditionRule(e.getAsJsonObject()));
                }
            }
        }
        return out;
    }

    public Tri evaluate(MacroContext ctx, ReasonSink reasons) {
        final List<ConditionRule> rules = this.rules();
        if (rules.isEmpty()) {
            return Tri.TRUE;
        }
        final boolean and = this.combinator() == Combinator.AND;
        boolean unknown = false;
        // unknown reasons only matter when they decide the result
        final List<String> collected = new ArrayList<>();
        for (ConditionRule rule : rules) {
            final Tri result = rule.evaluate(ctx, collected::add);
            if (result == Tri.UNKNOWN) {
                unknown = true;
            } else if (and && result == Tri.FALSE) {
                return Tri.FALSE;
            } else if (!and && result == Tri.TRUE) {
                return Tri.TRUE;
            }
        }
        if (unknown) {
            collected.forEach(reasons::unknown);
            return Tri.UNKNOWN;
        }
        return and ? Tri.TRUE : Tri.FALSE;
    }

    public String describe() {
        final List<ConditionRule> rules = this.rules();
        if (rules.isEmpty()) {
            return "always";
        }
        final List<String> parts = new ArrayList<>();
        rules.forEach(r -> parts.add(r.describe()));
        return String.join(this.combinator() == Combinator.AND ? " and " : " or ", parts);
    }

}
