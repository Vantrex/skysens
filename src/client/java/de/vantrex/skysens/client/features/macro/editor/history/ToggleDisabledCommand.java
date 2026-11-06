package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Disables the nodes, or enables them all when every one of them already is disabled (the D key). */
public final class ToggleDisabledCommand implements EditorCommand {

    private final Workflow workflow;
    private final Map<String, Boolean> before = new LinkedHashMap<>();
    private final boolean target;

    public ToggleDisabledCommand(Workflow workflow, Collection<MacroNode> nodes) {
        this.workflow = workflow;
        boolean allDisabled = !nodes.isEmpty();
        for (MacroNode node : nodes) {
            this.before.put(node.id, node.disabled);
            allDisabled &= node.disabled;
        }
        this.target = !allDisabled;
    }

    public boolean isEmpty() {
        return this.before.isEmpty();
    }

    @Override
    public void apply(EditorState state) {
        state.show(this.workflow);
        for (MacroNode node : this.workflow.nodes) {
            if (this.before.containsKey(node.id)) {
                node.disabled = this.target;
            }
        }
    }

    @Override
    public void revert(EditorState state) {
        state.show(this.workflow);
        for (MacroNode node : this.workflow.nodes) {
            final Boolean old = this.before.get(node.id);
            if (old != null) {
                node.disabled = old;
            }
        }
    }

    @Override
    public String label() {
        return this.target ? "Disable node" : "Enable node";
    }

}
