package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Follows the wires from a trigger to an action. Trigger to action only for now; logic nodes come later. */
public final class GraphWalker {

    private static final int MAX_DEPTH = 64;

    public enum End { ACTION, NO_ACTION, DISABLED, INVALID }

    /** The path one trigger takes. {@code action} is set only when {@code end} is ACTION. */
    public record PathResult(TriggerRef ref, List<String> nodePath, End end, @Nullable MacroNode action,
                             @Nullable ActionNodeType actionType) {
    }

    private GraphWalker() {
    }

    public static PathResult walk(TriggerRef ref) {
        final Workflow workflow = ref.workflow();
        final List<String> path = new ArrayList<>();
        MacroNode node = ref.trigger();
        path.add(node.id);
        String port = "main";
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            final Connection wire = findWire(workflow, node.id, port);
            if (wire == null) {
                return new PathResult(ref, path, End.NO_ACTION, null, null);
            }
            final MacroNode next = findNode(workflow, wire.to);
            if (next == null) {
                return new PathResult(ref, path, End.INVALID, null, null);
            }
            path.add(next.id);
            if (next.disabled) {
                return new PathResult(ref, path, End.DISABLED, null, null);
            }
            final NodeType type = NodeTypes.resolve(next.type);
            if (type instanceof ActionNodeType action) {
                return new PathResult(ref, path, End.ACTION, next, action);
            }
            // Anything else (unknown node, logic nodes before they exist) ends the path as invalid.
            return new PathResult(ref, path, End.INVALID, null, null);
        }
        return new PathResult(ref, path, End.INVALID, null, null);
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
