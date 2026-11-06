package de.vantrex.skysens.client.features.macro.editor.field;

import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;

import java.util.List;
import java.util.function.Consumer;

/**
 * What a parameter field needs to know about where it lives.
 *
 * @param all      every workflow (for keybind conflict hints)
 * @param changed  called with the param key after any edit
 * @param describe whether to show the spec description under the label (top level only)
 */
public record FieldContext(List<Workflow> all, Workflow workflow, MacroNode node, Consumer<String> changed, boolean describe) {

    public FieldContext nested(Consumer<String> changed) {
        return new FieldContext(this.all, this.workflow, this.node, changed, false);
    }

}
