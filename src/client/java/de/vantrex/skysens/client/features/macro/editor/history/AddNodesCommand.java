package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds nodes and the wires that go with them (a new node from the picker with its auto-connect wire, a paste, a
 * duplicate). A wire that takes over an output port replaces the wire the port had; undo brings that one back.
 */
public final class AddNodesCommand implements EditorCommand {

    private final Workflow workflow;
    private final List<MacroNode> nodes;
    private final List<Connection> connections;
    private final String label;
    private final List<Connection> replaced = new ArrayList<>();

    public AddNodesCommand(Workflow workflow, List<MacroNode> nodes, List<Connection> connections, String label) {
        this.workflow = workflow;
        this.nodes = List.copyOf(nodes);
        this.connections = List.copyOf(connections);
        this.label = label;
    }

    @Override
    public void apply(EditorState state) {
        state.show(this.workflow);
        this.workflow.nodes.addAll(this.nodes);
        this.replaced.clear();
        for (Connection added : this.connections) {
            for (Connection existing : new ArrayList<>(this.workflow.connections)) {
                if (existing.from.equals(added.from) && existing.port.equals(added.port)) {
                    this.workflow.connections.remove(existing);
                    this.replaced.add(existing);
                }
            }
            this.workflow.connections.add(added);
        }
        state.selectedNodes().clear();
        this.nodes.forEach(n -> state.selectedNodes().add(n.id));
        state.setSelectedWire(null);
    }

    @Override
    public void revert(EditorState state) {
        state.show(this.workflow);
        this.workflow.connections.removeAll(this.connections);
        this.workflow.connections.addAll(this.replaced);
        this.workflow.nodes.removeAll(this.nodes);
        for (MacroNode node : this.nodes) {
            state.selectedNodes().remove(node.id);
            if (node.id.equals(state.detailsNode())) {
                state.setDetailsNode(null);
            }
        }
        state.setSelectedWire(null);
    }

    @Override
    public String label() {
        return this.label;
    }

}
