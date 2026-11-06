package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo and redo stacks of the editor session (one linear history for canvas and macro list edits, kept while the
 * screen is open). A new command clears the redo stack; the undo stack is capped.
 */
public final class CommandHistory {

    public static final int LIMIT = 200;

    private final EditorState state;
    private final Deque<EditorCommand> undo = new ArrayDeque<>();
    private final Deque<EditorCommand> redo = new ArrayDeque<>();

    public CommandHistory(EditorState state) {
        this.state = state;
    }

    /** Applies the command and pushes it. */
    public void execute(EditorCommand command) {
        command.apply(this.state);
        this.record(command);
    }

    /** Pushes a command whose effect is already applied (a finished drag, a typed character). */
    public void record(EditorCommand command) {
        this.redo.clear();
        this.state.markDirty();
        final EditorCommand top = this.undo.peekFirst();
        if (top != null && top.mergeWith(command)) {
            return;
        }
        this.undo.addFirst(command);
        while (this.undo.size() > LIMIT) {
            this.undo.removeLast();
        }
    }

    /** Reverts the newest command; null if there is none. */
    public @Nullable EditorCommand undo() {
        final EditorCommand command = this.undo.pollFirst();
        if (command == null) {
            return null;
        }
        command.revert(this.state);
        this.redo.addFirst(command);
        this.afterChange();
        return command;
    }

    public @Nullable EditorCommand redo() {
        final EditorCommand command = this.redo.pollFirst();
        if (command == null) {
            return null;
        }
        command.apply(this.state);
        this.undo.addFirst(command);
        this.afterChange();
        return command;
    }

    private void afterChange() {
        this.state.markDirty();
        this.state.historyChanged();
    }

    public boolean canUndo() {
        return !this.undo.isEmpty();
    }

    public boolean canRedo() {
        return !this.redo.isEmpty();
    }

    public @Nullable String undoLabel() {
        final EditorCommand top = this.undo.peekFirst();
        return top == null ? null : top.label();
    }

    public @Nullable String redoLabel() {
        final EditorCommand top = this.redo.peekFirst();
        return top == null ? null : top.label();
    }

}
