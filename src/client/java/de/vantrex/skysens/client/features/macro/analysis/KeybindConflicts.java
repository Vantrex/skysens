package de.vantrex.skysens.client.features.macro.analysis;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.engine.InputChord;
import de.vantrex.skysens.client.features.macro.engine.InputDevice;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.trigger.HotkeyTriggerNode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds triggers bound to a key or mouse button that a key mapping (vanilla or another mod) also uses. That binding
 * only works when no macro action runs. Vanilla mappings have no modifiers, so a conflict counts whatever Ctrl/Shift/Alt
 * the trigger has.
 */
public final class KeybindConflicts {

    private KeybindConflicts() {
    }

    /** One WARNING per conflicting trigger and mapping, for the active workflows' enabled triggers. */
    public static List<NodeIssue> compute(List<Workflow> workflows) {
        final List<NodeIssue> out = new ArrayList<>();
        for (Workflow workflow : workflows) {
            if (workflow.active) {
                out.addAll(forWorkflow(workflow));
            }
        }
        return out;
    }

    public static List<NodeIssue> forWorkflow(Workflow workflow) {
        final List<NodeIssue> out = new ArrayList<>();
        final Options options = Minecraft.getInstance().options;
        if (options == null) {
            return out;
        }
        for (MacroNode node : workflow.nodes) {
            if (node.disabled || !(NodeTypes.resolve(node.type) instanceof HotkeyTriggerNode)) {
                continue;
            }
            final HotkeyTriggerNode.Spec spec = HotkeyTriggerNode.spec(node);
            if (spec == null) {
                continue;
            }
            final String keyName = keyName(spec.chord());
            for (KeyMapping mapping : options.keyMappings) {
                if (mapping.isUnbound() || !mapping.saveString().equals(keyName)) {
                    continue;
                }
                // Debug mappings (Show Chunk Boundaries, Advanced Tooltips, ...) only fire while F3 is held, so they
                // don't clash with a plain key. Only a macro on F3 itself really conflicts with them.
                if (mapping.getCategory() == KeyMapping.Category.DEBUG && !isDebugModifierKey(spec.chord())) {
                    continue;
                }
                final String mappingName = Component.translatable(mapping.getName()).getString();
                final String keyOnly = keyOnlyName(spec.chord());
                final String message = spec.chord().mods() == 0
                        ? keyOnly + " is also bound to " + mappingName + ": that binding only works when no macro action runs"
                        : spec.chord().displayName() + " also triggers " + mappingName + " (it ignores modifiers): that binding only works when no macro action runs";
                out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id, message));
            }
        }
        return out;
    }

    private static String keyName(InputChord chord) {
        final InputConstants.Type type = chord.device() == InputDevice.MOUSE ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
        return type.getOrCreate(chord.code()).getName();
    }

    private static boolean isDebugModifierKey(InputChord chord) {
        return chord.device() == InputDevice.KEYBOARD && chord.code() == GLFW_KEY_F3;
    }

    private static final int GLFW_KEY_F3 = 292;

    private static String keyOnlyName(InputChord chord) {
        return new InputChord(chord.device(), chord.code(), 0).displayName();
    }

}
