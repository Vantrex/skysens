package de.vantrex.skysens.client.features.macro.editor.clipboard;

import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.IdRemap;
import de.vantrex.skysens.client.features.macro.model.MacroDocument;
import de.vantrex.skysens.client.features.macro.model.MacroJson;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copy, cut, paste, export and import through the system clipboard, using the macro JSON envelope. Copied text is
 * always parsed again on paste, so pasted nodes are fresh objects with new ids.
 */
public final class MacroClipboard {

    public static final int MAX_NODES = 500;
    public static final int MAX_CHARS = 256 * 1024;

    /** Nodes and the wires between them, ready to be added to a workflow. */
    public record Pasted(List<MacroNode> nodes, List<Connection> connections) {
    }

    private MacroClipboard() {
    }

    /** Puts the nodes and the wires between them on the clipboard. */
    public static void copyNodes(Workflow workflow, Collection<String> ids) {
        final MacroDocument doc = new MacroDocument();
        doc.kind = MacroDocument.KIND_NODES;
        for (MacroNode node : workflow.nodes) {
            if (ids.contains(node.id)) {
                doc.nodes.add(node);
            }
        }
        for (Connection c : workflow.connections) {
            if (ids.contains(c.from) && ids.contains(c.to)) {
                doc.connections.add(c);
            }
        }
        set(MacroJson.write(doc));
    }

    /** Puts one macro on the clipboard as a "workflows" envelope. */
    public static void exportWorkflow(Workflow workflow) {
        final MacroDocument doc = new MacroDocument();
        doc.workflows.add(workflow);
        set(MacroJson.write(doc));
    }

    /** Copies of the nodes (and the wires between them) with new ids, 30 units right and below the originals. */
    public static Pasted duplicateNodes(Workflow workflow, Collection<String> ids) {
        final MacroDocument doc = new MacroDocument();
        doc.kind = MacroDocument.KIND_NODES;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (MacroNode node : workflow.nodes) {
            if (ids.contains(node.id)) {
                doc.nodes.add(node);
                minX = Math.min(minX, node.x);
                minY = Math.min(minY, node.y);
            }
        }
        for (Connection c : workflow.connections) {
            if (ids.contains(c.from) && ids.contains(c.to)) {
                doc.connections.add(c);
            }
        }
        if (doc.nodes.isEmpty()) {
            return new Pasted(List.of(), List.of());
        }
        try {
            return nodesFor(MacroJson.read(MacroJson.write(doc)), minX + 30, minY + 30);
        } catch (MacroJson.MacroFormatException e) {
            return new Pasted(List.of(), List.of());
        }
    }

    /** The toast text for a failed paste or import. Never leaks internals. */
    public static String describeFailure(Exception e) {
        return describeFailure(e, "paste");
    }

    /** The toast text for a failed paste or import. Never leaks internals. */
    public static String describeFailure(Exception e, String action) {
        final String message = e.getMessage() == null ? "" : e.getMessage();
        if (e instanceof MacroJson.MacroFormatException && (message.startsWith("Made with") || message.startsWith("Too many")
                || message.contains("too large") || message.contains("empty"))) {
            return "Can not " + action + ": " + message;
        }
        return "Clipboard does not contain SkySens macro JSON";
    }

    /** Parses the clipboard. Throws with a user-readable message when it is not (usable) macro JSON. */
    public static MacroDocument read() throws MacroJson.MacroFormatException {
        final String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (text == null || text.isBlank()) {
            throw new MacroJson.MacroFormatException("The clipboard is empty");
        }
        if (text.length() > MAX_CHARS) {
            throw new MacroJson.MacroFormatException("The clipboard content is too large");
        }
        final MacroDocument doc = MacroJson.read(text);
        int nodes = doc.nodes.size();
        for (Workflow workflow : doc.workflows) {
            nodes += workflow.nodes.size();
        }
        if (nodes > MAX_NODES) {
            throw new MacroJson.MacroFormatException("Too many nodes (more than " + MAX_NODES + ")");
        }
        return doc;
    }

    /**
     * The nodes of a document with new ids, moved so the top left of their bounding box lands on the world point
     * (snapped to the 10 unit grid). A "workflows" document contributes the nodes of all its workflows.
     */
    public static Pasted nodesFor(MacroDocument doc, double worldX, double worldY) {
        final List<MacroNode> nodes = new ArrayList<>(doc.nodes);
        final List<Connection> connections = new ArrayList<>(doc.connections);
        for (Workflow workflow : doc.workflows) {
            nodes.addAll(workflow.nodes);
            connections.addAll(workflow.connections);
        }
        // only wires between the pasted nodes survive
        final java.util.Set<String> ids = new java.util.HashSet<>();
        nodes.forEach(n -> ids.add(n.id));
        connections.removeIf(c -> !ids.contains(c.from) || !ids.contains(c.to));
        IdRemap.nodes(nodes, connections);
        if (!nodes.isEmpty()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            for (MacroNode node : nodes) {
                minX = Math.min(minX, node.x);
                minY = Math.min(minY, node.y);
            }
            final int dx = (int) (Math.round(worldX / 10.0) * 10) - minX;
            final int dy = (int) (Math.round(worldY / 10.0) * 10) - minY;
            for (MacroNode node : nodes) {
                node.x += dx;
                node.y += dy;
            }
        }
        return new Pasted(nodes, connections);
    }

    /** The macros of a document as new, switched-off workflows with new ids and unique names (Import, templates). */
    public static List<Workflow> workflowsFor(MacroDocument doc, List<Workflow> existing) {
        final List<Workflow> source = new ArrayList<>(doc.workflows);
        if (source.isEmpty() && !doc.nodes.isEmpty()) {
            final Workflow workflow = new Workflow();
            workflow.name = "Imported nodes";
            workflow.nodes = new ArrayList<>(doc.nodes);
            workflow.connections = new ArrayList<>(doc.connections);
            source.add(workflow);
        }
        final List<Workflow> taken = new ArrayList<>(existing);
        final List<Workflow> out = new ArrayList<>();
        for (Workflow workflow : source) {
            IdRemap.workflow(workflow);
            workflow.active = false;
            workflow.name = GraphEdit.uniqueName(workflow.name, taken);
            taken.add(workflow);
            out.add(workflow);
        }
        return out;
    }

    private static void set(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

}
