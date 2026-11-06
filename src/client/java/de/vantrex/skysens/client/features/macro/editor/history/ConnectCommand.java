package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import org.jetbrains.annotations.Nullable;

/** Adds a wire; the wire the output port already had (if any) is replaced and comes back on undo. */
public final class ConnectCommand implements EditorCommand {

    private final Workflow workflow;
    private final Connection connection;
    private final @Nullable Connection replaced;

    public ConnectCommand(Workflow workflow, Connection connection) {
        this.workflow = workflow;
        this.connection = connection;
        Connection existing = null;
        for (Connection c : workflow.connections) {
            if (c.from.equals(connection.from) && c.port.equals(connection.port)) {
                existing = c;
            }
        }
        this.replaced = existing;
    }

    @Override
    public void apply(EditorState state) {
        state.show(this.workflow);
        if (this.replaced != null) {
            this.workflow.connections.remove(this.replaced);
        }
        this.workflow.connections.add(this.connection);
        state.setSelectedWire(null);
    }

    @Override
    public void revert(EditorState state) {
        state.show(this.workflow);
        this.workflow.connections.remove(this.connection);
        if (this.replaced != null) {
            this.workflow.connections.add(this.replaced);
        }
        state.setSelectedWire(null);
    }

    @Override
    public String label() {
        return "Connect";
    }

}
