package de.vantrex.skysens.client.features.macro.node.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.condition.ConditionRule;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ConditionTypes;
import de.vantrex.skysens.client.features.macro.condition.RuleGroup;
import de.vantrex.skysens.client.features.macro.condition.RuleResult;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.PortKind;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the path in two. TRUE follows "true", FALSE follows "false". UNKNOWN follows neither: the path ends and the
 * press passes through.
 */
public final class IfNode implements LogicNodeType {

    public static final String ID = "logic.if";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "If";
    }

    @Override
    public String icon() {
        return "?";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("combinator", "Combine with", "All conditions (AND) or any condition (OR)", ParamKind.ENUM, new JsonPrimitive("AND"),
                        () -> List.of(new Choice("AND", "All (AND)"), new Choice("OR", "Any (OR)")), null),
                ParamSpec.of("conditions", "Conditions", "An empty list is always true", ParamKind.RULE_LIST, new JsonArray()));
    }

    @Override
    public List<Port> outputs(MacroNode node) {
        return List.of(new Port("true", "True", PortKind.OUTPUT), new Port("false", "False", PortKind.OUTPUT));
    }

    @Override
    public String summary(MacroNode node) {
        return new RuleGroup(node.params).describe();
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        validateGroup(new RuleGroup(node.params), node.id, out);
    }

    static void validateGroup(RuleGroup group, String nodeId, List<NodeIssue> out) {
        for (ConditionRule rule : group.rules()) {
            final ConditionType type = ConditionTypes.get(rule.type());
            if (type == null) {
                out.add(new NodeIssue(NodeIssue.Severity.ERROR, nodeId, "Unknown condition type \"" + rule.type() + "\""));
                continue;
            }
            final List<String> problems = new ArrayList<>();
            type.validate(rule.params(), problems);
            problems.forEach(p -> out.add(new NodeIssue(NodeIssue.Severity.WARNING, nodeId, type.displayName() + ": " + p)));
            if (rule.negate() && type.trackerBacked()) {
                out.add(new NodeIssue(NodeIssue.Severity.INFO, nodeId,
                        "Negated \"" + type.displayName() + "\" doesn't match while the state is unknown (e.g. right after joining)"));
            }
        }
    }

    @Override
    public Route route(MacroNode node, MacroContext ctx) {
        final RuleGroup group = new RuleGroup(node.params);
        final List<String> reasons = new ArrayList<>();
        final Tri result = group.evaluate(ctx, reasons::add);
        final String reason = result == Tri.UNKNOWN ? String.join("; ", reasons) : null;
        final List<RuleResult> results = List.of(new RuleResult("if", result, reason));
        return switch (result) {
            case TRUE -> new Route("true", results, List.of("true"), false);
            case FALSE -> new Route("false", results, List.of(), false);
            case UNKNOWN -> new Route(null, results, List.of(), true);
        };
    }

}
