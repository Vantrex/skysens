package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;

/** One hotkey trigger of an active workflow. */
public record TriggerRef(int workflowIndex, Workflow workflow, MacroNode trigger, InputChord chord, PressMode mode, boolean inGuis) {
}
