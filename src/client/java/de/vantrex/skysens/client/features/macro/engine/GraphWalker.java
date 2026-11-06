package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.condition.RuleResult;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.logic.LogicNodeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Follows the wires from a trigger through If / Switch nodes to an action. */
public final class GraphWalker {

    private static final int MAX_DEPTH = 64;

    /** END_UNKNOWN: an If / Switch could not decide because a condition was UNKNOWN. */
    public enum End { ACTION, NO_ACTION, DISABLED, INVALID, END_UNKNOWN }

    /** What one logic node decided: its rule results, the port taken and every output whose rule matched. */
    public record LogicStep(String nodeId, String nodeName, List<RuleResult> results, @Nullable String port, List<String> matchedPorts) {
        public boolean hasSeveralMatches() {
            return this.matchedPorts.size() > 1;
        }
    }

    /** The path one trigger takes. {@code action} is set only when {@code end} is ACTION. */
    public record PathResult(TriggerRef ref, List<String> nodePath, End end, @Nullable MacroNode action,
                             @Nullable ActionNodeType actionType, List<LogicStep> steps) {

        /** The first UNKNOWN reason of the logic steps, null if none. */
        public @Nullable String unknownReason() {
            for (LogicStep step : this.steps) {
                for (RuleResult result : step.results()) {
                    if (result.value() == Tri.UNKNOWN) {
                        return result.unknownReason();
                    }
                }
            }
            return null;
        }

        /** Short text such as "Context warp -> Send Command \"/warp home\"". */
        public String describe() {
            if (this.action == null || this.actionType == null) {
                return this.ref.workflow().name;
            }
            return this.ref.workflow().name + " -> " + this.actionType.displayName() + " \"" + this.actionType.summary(this.action) + "\"";
        }
    }

    private GraphWalker() {
    }

    public static PathResult walk(TriggerRef ref, MacroContext ctx) {
        final Workflow workflow = ref.workflow();
        final List<String> path = new ArrayList<>();
        final List<LogicStep> steps = new ArrayList<>();
        MacroNode node = ref.trigger();
        path.add(node.id);
        String port = "main";
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            final Connection wire = findWire(workflow, node.id, port);
            if (wire == null) {
                return new PathResult(ref, path, End.NO_ACTION, null, null, steps);
            }
            final MacroNode next = findNode(workflow, wire.to);
            if (next == null) {
                return new PathResult(ref, path, End.INVALID, null, null, steps);
            }
            path.add(next.id);
            if (next.disabled) {
                return new PathResult(ref, path, End.DISABLED, null, null, steps);
            }
            final NodeType type = NodeTypes.resolve(next.type);
            if (type instanceof ActionNodeType action) {
                return new PathResult(ref, path, End.ACTION, next, action, steps);
            }
            if (type instanceof LogicNodeType logic) {
                final LogicNodeType.Route route = logic.route(next, ctx);
                steps.add(new LogicStep(next.id, logic.displayName(), route.results(), route.port(), route.matchedPorts()));
                if (route.port() == null) {
                    return new PathResult(ref, path, route.unknown() ? End.END_UNKNOWN : End.NO_ACTION, null, null, steps);
                }
                node = next;
                port = route.port();
                continue;
            }
            // Unknown node types and anything else that cannot be walked through.
            return new PathResult(ref, path, End.INVALID, null, null, steps);
        }
        return new PathResult(ref, path, End.INVALID, null, null, steps);
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

}
