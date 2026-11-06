package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.Workflow;

/** Removes one wire. */
public final class DisconnectCommand implements EditorCommand {

    private final Workflow workflow;
    private final Connection connection;
    private final int index;

    public DisconnectCommand(Workflow workflow, Connection connection, int index) {
        this.workflow = workflow;
        this.connection = connection;
        this.index = Math.max(0, index);
    }

    @Override
    public void apply(EditorState state) {
        state.show(this.workflow);
        this.workflow.connections.remove(this.connection);
        state.setSelectedWire(null);
    }

    @Override
    public void revert(EditorState state) {
        state.show(this.workflow);
        this.workflow.connections.add(Math.min(this.index, this.workflow.connections.size()), this.connection);
        state.setSelectedWire(null);
    }

    @Override
    public String label() {
        return "Disconnect";
    }

}
