package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;

import java.util.List;

/** Several commands that undo and redo as one step (reconnecting a picked-up wire, say). */
public final class CompositeCommand implements EditorCommand {

    private final String label;
    private final List<EditorCommand> parts;

    public CompositeCommand(String label, List<EditorCommand> parts) {
        this.label = label;
        this.parts = List.copyOf(parts);
    }

    @Override
    public void apply(EditorState state) {
        for (EditorCommand part : this.parts) {
            part.apply(state);
        }
    }

    @Override
    public void revert(EditorState state) {
        for (int i = this.parts.size() - 1; i >= 0; i--) {
            this.parts.get(i).revert(state);
        }
    }

    @Override
    public String label() {
        return this.label;
    }

    @Override
    public boolean commitsImmediately() {
        return this.parts.stream().anyMatch(EditorCommand::commitsImmediately);
    }

}
