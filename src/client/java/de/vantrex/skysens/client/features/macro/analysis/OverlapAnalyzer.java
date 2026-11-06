package de.vantrex.skysens.client.features.macro.analysis;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.condition.ConditionRule;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ConditionTypes;
import de.vantrex.skysens.client.features.macro.condition.RuleGroup;
import de.vantrex.skysens.client.features.macro.engine.PressMode;
import de.vantrex.skysens.client.features.macro.engine.TriggerIndex;
import de.vantrex.skysens.client.features.macro.engine.TriggerRef;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.logic.IfNode;
import de.vantrex.skysens.client.features.macro.node.logic.SwitchNode;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The obvious overlaps only: the same key and press mode in several triggers, a trigger whose path always reaches an
 * action (so later triggers on the key are never reached), and Switch rules that repeat an earlier rule or match
 * everything. Semantic overlap between different conditions is not analysed; the press-time warning covers it. An
 * empty group is the only "always true": {@code x OR not x} is deliberately not treated as always true.
 */
final class OverlapAnalyzer {

    private static final int MAX_DEPTH = 64;

    private OverlapAnalyzer() {
    }

    static void analyze(List<Workflow> workflows, Map<String, List<NodeIssue>> issues) {
        final TriggerIndex index = TriggerIndex.build(workflows);
        for (TriggerIndex.BindingGroup group : index.groups()) {
            for (PressMode mode : PressMode.values()) {
                final List<TriggerRef> refs = group.forMode(mode);
                duplicates(refs, issues);
                shadowed(refs, issues);
            }
            if (group.isDeferred()) {
                for (TriggerRef ref : group.single()) {
                    add(issues, ref, NodeIssue.Severity.INFO,
                            "Fires late (after the double tap / long hold window) because this key also has such a trigger, and never consumes the key");
                }
            }
        }
        for (Workflow workflow : workflows) {
            for (MacroNode node : workflow.nodes) {
                if (NodeTypes.resolve(node.type) instanceof SwitchNode) {
                    switchRules(workflow, node, issues);
                }
            }
        }
    }

    // ------------------------------------------------------------------ triggers

    private static void duplicates(List<TriggerRef> refs, Map<String, List<NodeIssue>> issues) {
        if (refs.size() < 2) {
            return;
        }
        for (int i = 0; i < refs.size(); i++) {
            final TriggerRef self = refs.get(i);
            final List<String> others = new ArrayList<>();
            for (int j = 0; j < refs.size(); j++) {
                if (j == i) {
                    continue;
                }
                final TriggerRef other = refs.get(j);
                others.add(other.workflow() == self.workflow() ? "another trigger of this macro (#" + (j + 1) + ")"
                        : "\"" + other.workflow().name + "\" (#" + (j + 1) + ")");
            }
            add(issues, self, NodeIssue.Severity.WARNING, "Same key and press as " + String.join(", ", others)
                    + ". Runs as #" + (i + 1) + " of " + refs.size() + ", the first match wins");
        }
    }

    private static void shadowed(List<TriggerRef> refs, Map<String, List<NodeIssue>> issues) {
        for (int j = 1; j < refs.size(); j++) {
            final TriggerRef later = refs.get(j);
            for (int i = 0; i < j; i++) {
                final TriggerRef earlier = refs.get(i);
                // an earlier trigger that only fires outside inventories doesn't shadow one that also fires inside them
                if ((earlier.inGuis() || !later.inGuis()) && alwaysReachesAction(earlier)) {
                    add(issues, later, NodeIssue.Severity.WARNING, "Never reached: \"" + earlier.workflow().name
                            + "\" always handles " + later.chord().displayName());
                    break;
                }
            }
        }
    }

    /** Whether the trigger's path hits an action without any real condition on the way. */
    private static boolean alwaysReachesAction(TriggerRef ref) {
        final Workflow workflow = ref.workflow();
        MacroNode node = ref.trigger();
        String port = "main";
        final Set<String> visited = new HashSet<>();
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            if (!visited.add(node.id + "#" + port)) {
                return false;
            }
            final Connection wire = findWire(workflow, node.id, port);
            if (wire == null) {
                return false;
            }
            final MacroNode next = findNode(workflow, wire.to);
            if (next == null || next.disabled) {
                return false;
            }
            final NodeType type = NodeTypes.resolve(next.type);
            if (type instanceof ActionNodeType) {
                return true;
            }
            final String nextPort = alwaysPort(type, next);
            if (nextPort == null) {
                return false;
            }
            node = next;
            port = nextPort;
        }
        return false;
    }

    /** The output a logic node always takes (empty If group, empty first Switch rule), null if it depends on state. */
    private static @Nullable String alwaysPort(NodeType type, MacroNode node) {
        if (type instanceof IfNode) {
            return new RuleGroup(node.params).rules().isEmpty() ? "true" : null;
        }
        if (type instanceof SwitchNode) {
            final List<RuleGroup> rules = SwitchNode.rules(node);
            if (rules.isEmpty()) {
                return Params.getBool(node.params, "fallback", true) ? SwitchNode.FALLBACK : null;
            }
            return rules.get(0).rules().isEmpty() ? SwitchNode.portOf(rules.get(0)) : null;
        }
        return null;
    }

    private static void add(Map<String, List<NodeIssue>> issues, TriggerRef ref, NodeIssue.Severity severity, String message) {
        issues.computeIfAbsent(ref.workflow().id, k -> new ArrayList<>()).add(new NodeIssue(severity, ref.trigger().id, message));
    }

    private static @Nullable Connection findWire(Workflow workflow, String from, String port) {
        for (Connection c : workflow.connections) {
            if (from.equals(c.from) && port.equals(c.port)) {
                return c;
            }
        }
        return null;
    }

    private static @Nullable MacroNode findNode(Workflow workflow, String id) {
        for (MacroNode n : workflow.nodes) {
            if (n.id.equals(id)) {
                return n;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ switch rules

    private static void switchRules(Workflow workflow, MacroNode node, Map<String, List<NodeIssue>> issues) {
        final List<RuleGroup> rules = SwitchNode.rules(node);
        final List<NodeIssue> out = issues.computeIfAbsent(workflow.id, k -> new ArrayList<>());
        final Map<String, Integer> seen = new HashMap<>();
        int firstEmpty = -1;
        for (int i = 0; i < rules.size(); i++) {
            final RuleGroup rule = rules.get(i);
            if (firstEmpty >= 0) {
                out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id,
                        "Rule " + (i + 1) + " never matches: rule " + (firstEmpty + 1) + " has no conditions and always matches"));
                continue;
            }
            if (rule.rules().isEmpty()) {
                firstEmpty = i;
                if (i < rules.size() - 1) {
                    out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id,
                            "Rule " + (i + 1) + " always matches (no conditions), so the rules after it are unreachable"));
                }
                continue;
            }
            final Integer earlier = seen.putIfAbsent(ruleKey(rule), i);
            if (earlier != null) {
                out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id,
                        "Rule " + (i + 1) + " never matches: same conditions as rule " + (earlier + 1)));
            }
        }
    }

    /** Equal keys mean identical conditions with the same combinator, whatever their order. */
    private static String ruleKey(RuleGroup group) {
        final Set<String> parts = new TreeSet<>();
        for (ConditionRule rule : group.rules()) {
            final ConditionType type = ConditionTypes.get(rule.type());
            final String key;
            if (type == null) {
                key = rule.type() + ":" + Params.canonical(rule.params());
            } else {
                // defaults first, so a rule saved before a param existed equals a fresh one
                final JsonObject merged = ParamSpec.defaults(type.params());
                rule.params().entrySet().forEach(e -> merged.add(e.getKey(), e.getValue()));
                key = type.canonicalKey(merged);
            }
            parts.add((rule.negate() ? "not " : "") + key);
        }
        return (parts.size() > 1 ? group.combinator().name() : "-") + "|" + String.join("|", parts);
    }

}
