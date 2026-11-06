package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Deletes nodes together with their wires. Undo puts the very same nodes and wires back at their old positions. */
public final class DeleteNodesCommand implements EditorCommand {

    private final Workflow workflow;
    private final List<MacroNode> nodes = new ArrayList<>();
    private final List<Integer> nodeIndices = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private final List<Integer> connectionIndices = new ArrayList<>();

    public DeleteNodesCommand(Workflow workflow, Collection<String> ids) {
        this.workflow = workflow;
        for (int i = 0; i < workflow.nodes.size(); i++) {
            if (ids.contains(workflow.nodes.get(i).id)) {
                this.nodes.add(workflow.nodes.get(i));
                this.nodeIndices.add(i);
            }
        }
        for (int i = 0; i < workflow.connections.size(); i++) {
            final Connection c = workflow.connections.get(i);
            if (ids.contains(c.from) || ids.contains(c.to)) {
                this.connections.add(c);
                this.connectionIndices.add(i);
            }
        }
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    @Override
    public void apply(EditorState state) {
        state.show(this.workflow);
        this.workflow.nodes.removeAll(this.nodes);
        this.workflow.connections.removeAll(this.connections);
        for (MacroNode node : this.nodes) {
            if (node.id.equals(state.detailsNode())) {
                state.setDetailsNode(null);
            }
        }
        state.selectedNodes().clear();
        state.setSelectedWire(null);
    }

    @Override
    public void revert(EditorState state) {
        state.show(this.workflow);
        for (int i = 0; i < this.nodes.size(); i++) {
            this.workflow.nodes.add(Math.min(this.nodeIndices.get(i), this.workflow.nodes.size()), this.nodes.get(i));
        }
        for (int i = 0; i < this.connections.size(); i++) {
            this.workflow.connections.add(Math.min(this.connectionIndices.get(i), this.workflow.connections.size()), this.connections.get(i));
        }
        state.selectedNodes().clear();
        this.nodes.forEach(n -> state.selectedNodes().add(n.id));
        state.setSelectedWire(null);
    }

    @Override
    public String label() {
        return this.nodes.size() == 1 ? "Delete node" : "Delete " + this.nodes.size() + " nodes";
    }

}
