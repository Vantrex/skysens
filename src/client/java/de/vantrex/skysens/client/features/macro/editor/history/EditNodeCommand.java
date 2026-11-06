package de.vantrex.skysens.client.features.macro.editor.history;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A change to the name or params of a node (a typed character, a dropdown choice, a rule added or removed), recorded
 * after it happened. Consecutive edits of the same field within {@link #MERGE_MS} fold into one step. Wires that were
 * dropped because the node lost an output port (a Switch rule removed) come back on undo.
 */
public final class EditNodeCommand implements EditorCommand {

    public static final long MERGE_MS = 1000L;

    private static boolean breakNextMerge;

    /** The next recorded edit becomes its own undo step even if it would merge with the previous one. */
    public static void breakMerge() {
        breakNextMerge = true;
    }

    /** The editable state of a node. */
    public record Snapshot(@Nullable String name, JsonObject params) {

        public static Snapshot of(MacroNode node) {
            return new Snapshot(node.name, node.params.deepCopy());
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Snapshot s && Objects.equals(this.name, s.name) && this.params.equals(s.params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.params);
        }
    }

    private final Workflow workflow;
    private final String nodeId;
    private final String field;
    private final Snapshot before;
    private Snapshot after;
    private final List<Connection> removedWires;
    private long time;

    public EditNodeCommand(Workflow workflow, String nodeId, String field, Snapshot before, Snapshot after, List<Connection> removedWires) {
        this.workflow = workflow;
        this.nodeId = nodeId;
        this.field = field;
        this.before = before;
        this.after = after;
        this.removedWires = new ArrayList<>(removedWires);
        this.time = System.currentTimeMillis();
    }

    @Override
    public void apply(EditorState state) {
        state.show(this.workflow);
        this.set(this.after);
        this.workflow.connections.removeAll(this.removedWires);
        state.setSelectedWire(null);
    }

    @Override
    public void revert(EditorState state) {
        state.show(this.workflow);
        this.set(this.before);
        this.workflow.connections.addAll(this.removedWires);
        state.setSelectedWire(null);
    }

    private void set(Snapshot snapshot) {
        for (MacroNode node : this.workflow.nodes) {
            if (node.id.equals(this.nodeId)) {
                node.name = snapshot.name();
                node.params = snapshot.params().deepCopy();
                return;
            }
        }
    }

    @Override
    public boolean mergeWith(EditorCommand next) {
        if (breakNextMerge) {
            breakNextMerge = false;
            return false;
        }
        if (next instanceof EditNodeCommand other && other.workflow == this.workflow && other.nodeId.equals(this.nodeId)
                && other.field.equals(this.field) && other.time - this.time <= MERGE_MS) {
            this.after = other.after;
            for (Connection wire : other.removedWires) {
                if (!this.removedWires.contains(wire)) {
                    this.removedWires.add(wire);
                }
            }
            this.time = other.time;
            return true;
        }
        return false;
    }

    @Override
    public String label() {
        return "Edit node";
    }

}
