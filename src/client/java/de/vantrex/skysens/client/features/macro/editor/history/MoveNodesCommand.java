package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;

import java.util.Map;

/**
 * Moves (and for sticky notes resizes) nodes. Recorded after the drag has finished, so one drag is one step.
 * Bounds are {x, y, w, h}.
 */
public final class MoveNodesCommand implements EditorCommand {

    private final Workflow workflow;
    private final Map<String, int[]> before;
    private final Map<String, int[]> after;
    private final String label;

    public MoveNodesCommand(Workflow workflow, Map<String, int[]> before, Map<String, int[]> after, String label) {
        this.workflow = workflow;
        this.before = Map.copyOf(before);
        this.after = Map.copyOf(after);
        this.label = label;
    }

    /** The bounds {x, y, w, h} of a node right now. */
    public static int[] bounds(MacroNode node) {
        return new int[]{node.x, node.y, node.w, node.h};
    }

    @Override
    public void apply(EditorState state) {
        state.show(this.workflow);
        this.set(this.after);
    }

    @Override
    public void revert(EditorState state) {
        state.show(this.workflow);
        this.set(this.before);
    }

    private void set(Map<String, int[]> bounds) {
        for (MacroNode node : this.workflow.nodes) {
            final int[] b = bounds.get(node.id);
            if (b != null) {
                node.x = b[0];
                node.y = b[1];
                node.w = b[2];
                node.h = b[3];
            }
        }
    }

    @Override
    public String label() {
        return this.label;
    }

}
