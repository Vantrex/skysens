package de.vantrex.skysens.client.features.macro.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gives pasted or imported content fresh ids: nodes, the rules and conditions inside their params, and the Switch
 * ports ("rule:&lt;id&gt;") of the wires that refer to those rules.
 */
public final class IdRemap {

    private static final String RULE_PORT = "rule:";

    private IdRemap() {
    }

    /** New workflow id plus {@link #nodes} for its content. */
    public static void workflow(Workflow workflow) {
        workflow.id = Ids.next("wf");
        nodes(workflow.nodes, workflow.connections);
    }

    /** Remaps the nodes (in place) and updates the wires between them. Wires to nodes outside the list must be removed first. */
    public static void nodes(List<MacroNode> nodes, List<Connection> connections) {
        final Map<String, String> nodeIds = new HashMap<>();
        final Map<String, String> ruleIds = new HashMap<>();
        for (MacroNode node : nodes) {
            final String oldId = node.id;
            node.id = Ids.next("n");
            nodeIds.put(oldId, node.id);
            remapParams(node.params, oldId, ruleIds);
        }
        for (Connection c : connections) {
            final String oldFrom = c.from;
            c.from = nodeIds.getOrDefault(c.from, c.from);
            c.to = nodeIds.getOrDefault(c.to, c.to);
            if (c.port.startsWith(RULE_PORT)) {
                final String mapped = ruleIds.get(oldFrom + ":" + c.port.substring(RULE_PORT.length()));
                if (mapped != null) {
                    c.port = RULE_PORT + mapped;
                }
            }
        }
    }

    private static void remapParams(JsonElement element, String nodeId, Map<String, String> ruleIds) {
        if (element.isJsonObject()) {
            final JsonObject object = element.getAsJsonObject();
            final JsonElement id = object.get("id");
            if (id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString()) {
                final String old = id.getAsString();
                final int underscore = old.indexOf('_');
                final String fresh = Ids.next(underscore > 0 ? old.substring(0, underscore) : "r");
                ruleIds.put(nodeId + ":" + old, fresh);
                object.addProperty("id", fresh);
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                remapParams(entry.getValue(), nodeId, ruleIds);
            }
        } else if (element.isJsonArray()) {
            final JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                remapParams(child, nodeId, ruleIds);
            }
        }
    }

}
