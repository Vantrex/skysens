package de.vantrex.skysens.client.features.macro.editor.edit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.Ids;
import de.vantrex.skysens.client.features.macro.model.MacroJson;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.logic.IfNode;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.trigger.HotkeyTriggerNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Direct mutations of workflows for the editor (no undo history yet). Each output port holds at most one wire, because
 * the engine follows a single path per press: connecting an already connected output replaces its wire.
 */
public final class GraphEdit {

    public enum ConnectResult { OK, SAME_NODE, CYCLE, NO_INPUT, NO_PORT }

    private GraphEdit() {
    }

    public static @Nullable MacroNode find(Workflow workflow, @Nullable String id) {
        if (id == null) {
            return null;
        }
        for (MacroNode node : workflow.nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    public static MacroNode addNode(Workflow workflow, NodeType type, int x, int y) {
        final MacroNode node = new MacroNode();
        node.id = Ids.next("n");
        node.type = type.id();
        node.x = x;
        node.y = y;
        node.params = type.defaultParams();
        workflow.nodes.add(node);
        return node;
    }

    /** An If node holding one rule of the given condition type with default params (the picker's "Conditions" group). */
    public static MacroNode addConditionNode(Workflow workflow, ConditionType condition, int x, int y) {
        final NodeType ifType = NodeTypes.resolve(IfNode.ID);
        final MacroNode node = addNode(workflow, ifType, x, y);
        final JsonArray rules = new JsonArray();
        rules.add(newRule(condition));
        node.params.add("conditions", rules);
        return node;
    }

    public static JsonObject newRule(ConditionType type) {
        final JsonObject rule = new JsonObject();
        rule.addProperty("id", Ids.next("c"));
        rule.addProperty("type", type.id());
        rule.addProperty("negate", false);
        rule.add("params", ParamSpec.defaults(type.params()));
        return rule;
    }

    public static JsonObject newSwitchRule() {
        final JsonObject group = new JsonObject();
        group.addProperty("id", Ids.next("r"));
        group.addProperty("name", "");
        group.add("combinator", new JsonPrimitive("AND"));
        group.add("conditions", new JsonArray());
        return group;
    }

    // ------------------------------------------------------------------ wires

    /** Whether a wire from -> to would close a loop (to already leads back to from). */
    public static boolean wouldCycle(Workflow workflow, String from, String to) {
        if (from.equals(to)) {
            return true;
        }
        final Set<String> seen = new HashSet<>();
        final ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(to);
        while (!queue.isEmpty()) {
            final String id = queue.poll();
            if (!seen.add(id)) {
                continue;
            }
            for (Connection c : workflow.connections) {
                if (c.from.equals(id)) {
                    if (c.to.equals(from)) {
                        return true;
                    }
                    queue.add(c.to);
                }
            }
        }
        return false;
    }

    /** Wires {@code from.port -> to}, replacing the wire the output port had. Nothing changes unless the result is OK. */
    public static ConnectResult connect(Workflow workflow, String fromId, String port, String toId) {
        final MacroNode from = find(workflow, fromId);
        final MacroNode to = find(workflow, toId);
        if (from == null || to == null) {
            return ConnectResult.NO_PORT;
        }
        if (from == to) {
            return ConnectResult.SAME_NODE;
        }
        if (NodeTypes.resolve(to.type).inputs(to).isEmpty()) {
            return ConnectResult.NO_INPUT;
        }
        final List<Port> outputs = NodeTypes.resolve(from.type).outputs(from);
        if (outputs.stream().noneMatch(p -> p.id().equals(port))) {
            return ConnectResult.NO_PORT;
        }
        if (wouldCycle(workflow, fromId, toId)) {
            return ConnectResult.CYCLE;
        }
        workflow.connections.removeIf(c -> c.from.equals(fromId) && c.port.equals(port));
        workflow.connections.add(new Connection(fromId, port, toId));
        return ConnectResult.OK;
    }

    public static String describe(ConnectResult result) {
        return switch (result) {
            case OK -> "";
            case SAME_NODE -> "A node can't connect to itself";
            case CYCLE -> "Connection rejected: it would create a loop";
            case NO_INPUT -> "That node has no input";
            case NO_PORT -> "That output doesn't exist";
        };
    }

    public static void deleteNodes(Workflow workflow, Collection<String> ids) {
        workflow.nodes.removeIf(n -> ids.contains(n.id));
        workflow.connections.removeIf(c -> ids.contains(c.from) || ids.contains(c.to));
    }

    /** Drops the wires of output ports a node no longer has (after a Switch rule was removed, say). Known types only. */
    public static void pruneConnections(Workflow workflow, MacroNode node) {
        if (!NodeTypes.isKnown(node.type)) {
            return;
        }
        final Set<String> ports = new HashSet<>();
        NodeTypes.resolve(node.type).outputs(node).forEach(p -> ports.add(p.id()));
        workflow.connections.removeIf(c -> c.from.equals(node.id) && !ports.contains(c.port));
    }

    // ------------------------------------------------------------------ workflows

    /** A new macro with one unbound Hotkey trigger. */
    public static Workflow newWorkflow(List<Workflow> existing) {
        final Workflow workflow = new Workflow();
        workflow.id = Ids.next("wf");
        workflow.name = uniqueName("New macro", existing);
        final NodeType trigger = NodeTypes.resolve(HotkeyTriggerNode.ID);
        addNode(workflow, trigger, 60, 60);
        return workflow;
    }

    /** A deep copy with a new id and name, switched off so it doesn't fight its original for the key. */
    public static Workflow duplicate(Workflow source, List<Workflow> existing) {
        final Workflow copy = MacroJson.parseWorkflow(MacroJson.workflowToJson(source));
        copy.id = Ids.next("wf");
        copy.name = uniqueName(source.name + " (copy)", existing);
        copy.active = false;
        return copy;
    }

    private static String uniqueName(String base, List<Workflow> existing) {
        String name = base;
        int i = 2;
        while (true) {
            final String candidate = name;
            if (existing.stream().noneMatch(w -> w.name.equals(candidate))) {
                return name;
            }
            name = base + " " + i++;
        }
    }

}
