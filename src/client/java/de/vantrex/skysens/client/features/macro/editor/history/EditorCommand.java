package de.vantrex.skysens.client.features.macro.editor.history;

import de.vantrex.skysens.client.features.macro.editor.EditorState;

/** One undoable step of the editor. Commands keep exactly what they need to apply and revert themselves. */
public interface EditorCommand {

    void apply(EditorState state);

    void revert(EditorState state);

    /** Short text for the toast ("Move nodes"). */
    String label();

    /** Folds the next command into this one (typing in one field). Return true when it was absorbed. */
    default boolean mergeWith(EditorCommand next) {
        return false;
    }

    /** Whether applying or reverting this changes what the engine runs, so the editor saves right away (active toggle). */
    default boolean commitsImmediately() {
        return false;
    }

}
