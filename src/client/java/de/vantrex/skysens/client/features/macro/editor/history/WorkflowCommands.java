package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.Workflow;

import java.util.List;

/** The commands of the macro list: add (new, duplicate, import, template), delete, rename, reorder, switch on or off. */
public final class WorkflowCommands {

    private WorkflowCommands() {
    }

    /** Adds a macro at the index and shows it. */
    public static final class Add implements EditorCommand {

        private final Workflow workflow;
        private final int index;
        private final String label;

        public Add(Workflow workflow, int index, String label) {
            this.workflow = workflow;
            this.index = index;
            this.label = label;
        }

        @Override
        public void apply(EditorState state) {
            final List<Workflow> list = state.workflows();
            list.add(Math.min(this.index, list.size()), this.workflow);
            state.select(this.workflow);
        }

        @Override
        public void revert(EditorState state) {
            removeAndRefocus(state, this.workflow, this.index);
        }

        @Override
        public String label() {
            return this.label;
        }
    }

    public static final class Delete implements EditorCommand {

        private final Workflow workflow;
        private final int index;

        public Delete(Workflow workflow, int index) {
            this.workflow = workflow;
            this.index = index;
        }

        @Override
        public void apply(EditorState state) {
            removeAndRefocus(state, this.workflow, this.index);
        }

        @Override
        public void revert(EditorState state) {
            final List<Workflow> list = state.workflows();
            list.add(Math.min(this.index, list.size()), this.workflow);
            state.select(this.workflow);
        }

        @Override
        public String label() {
            return "Delete macro";
        }
    }

    private static void removeAndRefocus(EditorState state, Workflow workflow, int index) {
        final List<Workflow> list = state.workflows();
        list.remove(workflow);
        if (workflow == state.current()) {
            state.select(list.isEmpty() ? null : list.get(Math.min(index, list.size() - 1)));
        }
    }

    public static final class Rename implements EditorCommand {

        private final Workflow workflow;
        private final String before;
        private final String after;

        public Rename(Workflow workflow, String after) {
            this.workflow = workflow;
            this.before = workflow.name;
            this.after = after;
        }

        @Override
        public void apply(EditorState state) {
            this.workflow.name = this.after;
        }

        @Override
        public void revert(EditorState state) {
            this.workflow.name = this.before;
        }

        @Override
        public String label() {
            return "Rename macro";
        }
    }

    /** Moves a macro within the list (the evaluation order). */
    public static final class Reorder implements EditorCommand {

        private final Workflow workflow;
        private final int from;
        private final int to;

        public Reorder(Workflow workflow, int from, int to) {
            this.workflow = workflow;
            this.from = from;
            this.to = to;
        }

        @Override
        public void apply(EditorState state) {
            move(state.workflows(), this.workflow, this.to);
        }

        @Override
        public void revert(EditorState state) {
            move(state.workflows(), this.workflow, this.from);
        }

        private static void move(List<Workflow> list, Workflow workflow, int index) {
            list.remove(workflow);
            list.add(Math.max(0, Math.min(list.size(), index)), workflow);
        }

        @Override
        public String label() {
            return "Reorder macros";
        }
    }

    /** The Active switch. Saved at once, because the engine only sees committed changes. */
    public static final class ToggleActive implements EditorCommand {

        private final Workflow workflow;

        public ToggleActive(Workflow workflow) {
            this.workflow = workflow;
        }

        @Override
        public void apply(EditorState state) {
            this.workflow.active = !this.workflow.active;
        }

        @Override
        public void revert(EditorState state) {
            this.workflow.active = !this.workflow.active;
        }

        @Override
        public String label() {
            return "Switch macro on or off";
        }

        @Override
        public boolean commitsImmediately() {
            return true;
        }
    }

}
