package de.vantrex.skysens.client.features.macro.node.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.condition.ConditionRule;
import de.vantrex.skysens.client.features.macro.condition.ConditionTypes;
import de.vantrex.skysens.client.features.macro.condition.RuleGroup;
import de.vantrex.skysens.client.features.macro.condition.RuleResult;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.PortKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.ArrayList;
import java.util.List;

/**
 * One output per rule group plus an optional fallback. The first TRUE rule wins; UNKNOWN rules are skipped and
 * recorded. The fallback is taken only when every rule is FALSE; with an UNKNOWN rule and no TRUE one the path ends.
 */
public final class SwitchNode implements LogicNodeType {

    public static final String ID = "logic.switch";
    public static final String FALLBACK = "fallback";

    public static String portOf(RuleGroup rule) {
        return "rule:" + rule.id();
    }

    public static List<RuleGroup> rules(MacroNode node) {
        final List<RuleGroup> out = new ArrayList<>();
        final JsonElement rules = node.params.get("rules");
        if (rules != null && rules.isJsonArray()) {
            for (JsonElement e : rules.getAsJsonArray()) {
                if (e.isJsonObject()) {
                    out.add(new RuleGroup(e.getAsJsonObject()));
                }
            }
        }
        return out;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Switch";
    }

    @Override
    public String icon() {
        return "S";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                ParamSpec.of("rules", "Rules", "The first matching rule wins. Each rule has its own output", ParamKind.RULE_LIST, new JsonArray()),
                ParamSpec.of("fallback", "Fallback output", "Adds an output taken when no rule matches (not while a rule is unknown)", ParamKind.BOOL, new JsonPrimitive(true)));
    }

    @Override
    public List<Port> outputs(MacroNode node) {
        final List<Port> out = new ArrayList<>();
        int index = 1;
        for (RuleGroup rule : rules(node)) {
            out.add(new Port(portOf(rule), rule.name().isBlank() ? "Rule " + index : rule.name(), PortKind.OUTPUT));
            index++;
        }
        if (Params.getBool(node.params, "fallback", true)) {
            out.add(new Port(FALLBACK, "Else", PortKind.OUTPUT));
        }
        return out;
    }

    @Override
    public String summary(MacroNode node) {
        final int count = rules(node).size();
        return count + (count == 1 ? " rule" : " rules");
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        final List<RuleGroup> rules = rules(node);
        if (rules.isEmpty()) {
            out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id, "No rules, only the fallback can be taken"));
        }
        boolean tracker = false;
        for (RuleGroup rule : rules) {
            IfNode.validateGroup(rule, node.id, out);
            for (ConditionRule condition : rule.rules()) {
                tracker |= ConditionTypes.get(condition.type()) != null && ConditionTypes.get(condition.type()).trackerBacked();
            }
        }
        if (tracker && Params.getBool(node.params, "fallback", true)) {
            out.add(new NodeIssue(NodeIssue.Severity.INFO, node.id, "The fallback is skipped while any rule is unknown"));
        }
    }

    @Override
    public Route route(MacroNode node, MacroContext ctx) {
        final List<RuleResult> results = new ArrayList<>();
        final List<String> matched = new ArrayList<>();
        boolean unknown = false;
        for (RuleGroup rule : rules(node)) {
            final List<String> reasons = new ArrayList<>();
            final Tri result = rule.evaluate(ctx, reasons::add);
            results.add(new RuleResult(rule.id(), result, result == Tri.UNKNOWN ? String.join("; ", reasons) : null));
            if (result == Tri.TRUE) {
                matched.add(portOf(rule));
            } else if (result == Tri.UNKNOWN) {
                unknown = true;
            }
        }
        if (!matched.isEmpty()) {
            return new Route(matched.get(0), results, matched, false);
        }
        if (unknown) {
            return new Route(null, results, matched, true);
        }
        if (Params.getBool(node.params, "fallback", true)) {
            return new Route(FALLBACK, results, matched, false);
        }
        return new Route(null, results, matched, false);
    }

}
