package de.vantrex.skysens.client.features.macro.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * The one parser for the macro JSON format (repository, clipboard, templates). Parsing is defensive: missing or
 * wrongly typed values fall back to defaults, and keys this version does not know are kept in the model
 * ("extra") and written back, so nothing is lost through a load and save.
 */
public final class MacroJson {

    public static final String FORMAT = "skysens-macro";
    public static final int CURRENT_VERSION = 1;

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Set<String> WORKFLOW_KEYS = Set.of("id", "name", "active", "viewport", "nodes", "connections");
    private static final Set<String> NODE_KEYS = Set.of("id", "type", "name", "disabled", "x", "y", "w", "h", "params");
    private static final Set<String> CONNECTION_KEYS = Set.of("from", "port", "to");

    private MacroJson() {
    }

    /** Thrown for a document that is not a macro document or is newer than this version. */
    public static final class MacroFormatException extends Exception {
        public MacroFormatException(String message) {
            super(message);
        }
    }

    // ---- document (clipboard / templates)

    public static MacroDocument read(String json) throws MacroFormatException {
        final JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new MacroFormatException("Not valid JSON");
        }
        if (!parsed.isJsonObject()) {
            throw new MacroFormatException("Not a macro document");
        }
        final JsonObject root = parsed.getAsJsonObject();
        if (!FORMAT.equals(string(root, "format", null))) {
            throw new MacroFormatException("Not a SkySens macro document");
        }
        final int version = integer(root, "version", 1);
        if (version > CURRENT_VERSION) {
            throw new MacroFormatException("Made with a newer SkySens (format version " + version + ")");
        }
        MacroFormatUpgrader.upgrade(root, version);

        final MacroDocument doc = new MacroDocument();
        doc.kind = MacroDocument.KIND_NODES.equals(string(root, "kind", "")) ? MacroDocument.KIND_NODES : MacroDocument.KIND_WORKFLOWS;
        doc.workflows = parseWorkflows(array(root, "workflows"));
        doc.nodes = parseNodes(array(root, "nodes"));
        doc.connections = parseConnections(array(root, "connections"));
        return doc;
    }

    public static String write(MacroDocument doc) {
        final JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", CURRENT_VERSION);
        root.addProperty("kind", doc.kind);
        if (MacroDocument.KIND_NODES.equals(doc.kind)) {
            root.add("nodes", nodesToJson(doc.nodes));
            root.add("connections", connectionsToJson(doc.connections));
        } else {
            root.add("workflows", workflowsToJson(doc.workflows));
        }
        return PRETTY.toJson(root);
    }

    // ---- repository

    /** Parses the raw workflow objects stored in the repository. Entries that are not objects are skipped. */
    public static List<Workflow> parseWorkflows(@Nullable List<? extends JsonElement> raw) {
        final List<Workflow> out = new java.util.ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (JsonElement element : raw) {
            if (element != null && element.isJsonObject()) {
                out.add(parseWorkflow(element.getAsJsonObject()));
            }
        }
        return out;
    }

    public static List<JsonObject> workflowsToRaw(List<Workflow> workflows) {
        final List<JsonObject> out = new java.util.ArrayList<>();
        for (Workflow workflow : workflows) {
            out.add(workflowToJson(workflow));
        }
        return out;
    }

    // ---- workflow

    public static Workflow parseWorkflow(JsonObject json) {
        final Workflow wf = new Workflow();
        wf.id = string(json, "id", Ids.next("wf"));
        wf.name = string(json, "name", "Macro");
        wf.active = bool(json, "active", true);
        final JsonObject viewport = object(json, "viewport");
        if (viewport != null) {
            wf.viewport.x = number(viewport, "x", 0);
            wf.viewport.y = number(viewport, "y", 0);
            final double zoom = number(viewport, "zoom", 1.0);
            wf.viewport.zoom = zoom > 0 ? zoom : 1.0;
        }
        wf.nodes = parseNodes(array(json, "nodes"));
        wf.connections = parseConnections(array(json, "connections"));
        wf.extra = extras(json, WORKFLOW_KEYS);
        return wf;
    }

    public static JsonObject workflowToJson(Workflow wf) {
        final JsonObject json = new JsonObject();
        json.addProperty("id", wf.id);
        json.addProperty("name", wf.name);
        json.addProperty("active", wf.active);
        final JsonObject viewport = new JsonObject();
        viewport.addProperty("x", wf.viewport.x);
        viewport.addProperty("y", wf.viewport.y);
        viewport.addProperty("zoom", wf.viewport.zoom);
        json.add("viewport", viewport);
        json.add("nodes", nodesToJson(wf.nodes));
        json.add("connections", connectionsToJson(wf.connections));
        wf.extra.entrySet().forEach(e -> json.add(e.getKey(), e.getValue().deepCopy()));
        return json;
    }

    private static JsonArray workflowsToJson(List<Workflow> workflows) {
        final JsonArray array = new JsonArray();
        workflows.forEach(wf -> array.add(workflowToJson(wf)));
        return array;
    }

    // ---- nodes

    private static List<MacroNode> parseNodes(@Nullable JsonArray array) {
        final List<MacroNode> out = new java.util.ArrayList<>();
        if (array == null) {
            return out;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject json = element.getAsJsonObject();
            final MacroNode node = new MacroNode();
            node.id = string(json, "id", Ids.next("n"));
            node.type = string(json, "type", "");
            node.name = string(json, "name", null);
            node.disabled = bool(json, "disabled", false);
            node.x = (int) Math.round(number(json, "x", 0));
            node.y = (int) Math.round(number(json, "y", 0));
            node.w = (int) Math.round(number(json, "w", 0));
            node.h = (int) Math.round(number(json, "h", 0));
            final JsonObject params = object(json, "params");
            node.params = params != null ? params : new JsonObject();
            node.extra = extras(json, NODE_KEYS);
            out.add(node);
        }
        return out;
    }

    private static JsonArray nodesToJson(List<MacroNode> nodes) {
        final JsonArray array = new JsonArray();
        for (MacroNode node : nodes) {
            final JsonObject json = new JsonObject();
            json.addProperty("id", node.id);
            json.addProperty("type", node.type);
            if (node.name != null) {
                json.addProperty("name", node.name);
            }
            if (node.disabled) {
                json.addProperty("disabled", true);
            }
            json.addProperty("x", node.x);
            json.addProperty("y", node.y);
            if (node.w != 0) {
                json.addProperty("w", node.w);
            }
            if (node.h != 0) {
                json.addProperty("h", node.h);
            }
            json.add("params", node.params.deepCopy());
            node.extra.entrySet().forEach(e -> json.add(e.getKey(), e.getValue().deepCopy()));
            array.add(json);
        }
        return array;
    }

    // ---- connections

    private static List<Connection> parseConnections(@Nullable JsonArray array) {
        final List<Connection> out = new java.util.ArrayList<>();
        if (array == null) {
            return out;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject json = element.getAsJsonObject();
            final String from = string(json, "from", null);
            final String to = string(json, "to", null);
            if (from == null || to == null) {
                continue;
            }
            final Connection connection = new Connection(from, string(json, "port", "main"), to);
            connection.extra = extras(json, CONNECTION_KEYS);
            out.add(connection);
        }
        return out;
    }

    private static JsonArray connectionsToJson(List<Connection> connections) {
        final JsonArray array = new JsonArray();
        for (Connection connection : connections) {
            final JsonObject json = new JsonObject();
            json.addProperty("from", connection.from);
            json.addProperty("port", connection.port);
            json.addProperty("to", connection.to);
            connection.extra.entrySet().forEach(e -> json.add(e.getKey(), e.getValue().deepCopy()));
            array.add(json);
        }
        return array;
    }

    // ---- helpers

    private static JsonObject extras(JsonObject json, Set<String> known) {
        final JsonObject extra = new JsonObject();
        json.entrySet().stream().filter(e -> !known.contains(e.getKey()))
                .forEach(e -> extra.add(e.getKey(), e.getValue().deepCopy()));
        return extra;
    }

    private static @Nullable String string(JsonObject json, String key, @Nullable String def) {
        final JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    private static boolean bool(JsonObject json, String key, boolean def) {
        final JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : def;
    }

    private static int integer(JsonObject json, String key, int def) {
        return (int) Math.round(number(json, key, def));
    }

    private static double number(JsonObject json, String key, double def) {
        final JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : def;
    }

    private static @Nullable JsonObject object(JsonObject json, String key) {
        final JsonElement e = json.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static @Nullable JsonArray array(JsonObject json, String key) {
        final JsonElement e = json.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    private static List<JsonElement> list(@Nullable JsonArray array) {
        final List<JsonElement> out = new java.util.ArrayList<>();
        if (array != null) {
            array.forEach(out::add);
        }
        return out;
    }

    private static List<Workflow> parseWorkflows(@Nullable JsonArray array) {
        return parseWorkflows(list(array));
    }

}
