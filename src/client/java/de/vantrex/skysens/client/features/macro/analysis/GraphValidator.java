package de.vantrex.skysens.client.features.macro.analysis;

import de.vantrex.skysens.client.features.macro.action.placeholder.PlaceholderRegistry;
import de.vantrex.skysens.client.features.macro.action.placeholder.PlaceholderRenderer;
import de.vantrex.skysens.client.features.macro.condition.RuleGroup;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.logic.IfNode;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the problems of a set of workflows: per-node validation, placeholders, wiring, keybind conflicts and (through
 * {@link OverlapAnalyzer}) the obvious overlaps between triggers and Switch rules. Pure and cheap, but call it when
 * the graph changed, never every frame.
 */
public final class GraphValidator {

    private GraphValidator() {
    }

    /** Validates all workflows together (overlaps need the whole list, in evaluation order). */
    public static ValidationResult validate(List<Workflow> workflows) {
        final Map<String, List<NodeIssue>> issues = new LinkedHashMap<>();
        for (Workflow workflow : workflows) {
            issues.put(workflow.id, validateWorkflow(workflow));
        }
        OverlapAnalyzer.analyze(workflows, issues);
        final ValidationResult result = new ValidationResult();
        issues.forEach(result::put);
        return result;
    }

    private static List<NodeIssue> validateWorkflow(Workflow workflow) {
        final List<NodeIssue> out = new ArrayList<>();
        final Set<String> ids = new HashSet<>();
        final Set<String> hasIncoming = new HashSet<>();
        final Set<String> wired = new HashSet<>();
        for (Connection connection : workflow.connections) {
            hasIncoming.add(connection.to);
            wired.add(connection.from + "#" + connection.port);
        }
        for (MacroNode node : workflow.nodes) {
            ids.add(node.id);
        }
        for (MacroNode node : workflow.nodes) {
            final NodeType type = NodeTypes.resolve(node.type);
            type.validate(node, out);
            checkPlaceholders(node, type, out);
            if (type.category() == NodeCategory.NOTE || !NodeTypes.isKnown(node.type)) {
                continue;
            }
            if (type.category() == NodeCategory.TRIGGER) {
                if (!hasOutgoing(type, node, wired)) {
                    out.add(new NodeIssue(NodeIssue.Severity.INFO, node.id, "Not connected to anything, this trigger does nothing"));
                }
            } else if (!hasIncoming.contains(node.id)) {
                out.add(new NodeIssue(NodeIssue.Severity.INFO, node.id, "Nothing leads here, so this node never runs"));
            }
            if (type.category() == NodeCategory.LOGIC) {
                for (Port port : type.outputs(node)) {
                    if (!wired.contains(node.id + "#" + port.id())) {
                        out.add(new NodeIssue(NodeIssue.Severity.INFO, node.id,
                                "Output \"" + (port.label().isBlank() ? port.id() : port.label()) + "\" is not connected, nothing happens on that path"));
                    }
                }
                if (type instanceof IfNode && new RuleGroup(node.params).rules().isEmpty()) {
                    out.add(new NodeIssue(NodeIssue.Severity.INFO, node.id, "Always true: an If without conditions takes the true output"));
                }
            }
        }
        for (Connection connection : workflow.connections) {
            if (!ids.contains(connection.from) || !ids.contains(connection.to)) {
                out.add(new NodeIssue(NodeIssue.Severity.WARNING, connection.from, "Wire " + connection.from + " to " + connection.to + " points at a missing node"));
            }
        }
        if (workflow.active) {
            out.addAll(KeybindConflicts.forWorkflow(workflow));
        }
        return out;
    }

    private static boolean hasOutgoing(NodeType type, MacroNode node, Set<String> wired) {
        for (Port port : type.outputs(node)) {
            if (wired.contains(node.id + "#" + port.id())) {
                return true;
            }
        }
        return false;
    }

    private static void checkPlaceholders(MacroNode node, NodeType type, List<NodeIssue> out) {
        for (ParamSpec spec : type.params()) {
            if (spec.kind() != ParamKind.TEMPLATE_TEXT) {
                continue;
            }
            for (String path : PlaceholderRenderer.paths(Params.getString(node.params, spec.key(), ""))) {
                if (!PlaceholderRegistry.isKnown(path)) {
                    out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id, "Unknown placeholder {{ " + path + " }}"));
                }
            }
        }
    }

}
