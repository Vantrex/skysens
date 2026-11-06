package de.vantrex.skysens.client.features.macro.node.trigger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.engine.InputChord;
import de.vantrex.skysens.client.features.macro.engine.PressMode;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.PortKind;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Starts a macro when a key or mouse button chord is pressed. */
public final class HotkeyTriggerNode implements NodeType {

    public static final String ID = "trigger.hotkey";

    /** What a trigger node is bound to. */
    public record Spec(InputChord chord, PressMode mode, boolean inGuis) {
    }

    /** The binding of a trigger node, or null when it is unbound or malformed. */
    public static @Nullable Spec spec(MacroNode node) {
        final InputChord chord = InputChord.fromJson(node.params.get("input"));
        if (chord == null) {
            return null;
        }
        return new Spec(chord, Params.getEnum(node.params, "mode", PressMode.class, PressMode.SINGLE),
                Params.getBool(node.params, "inGuis", false));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Hotkey";
    }

    @Override
    public NodeCategory category() {
        return NodeCategory.TRIGGER;
    }

    @Override
    public String icon() {
        return "K";
    }

    @Override
    public List<ParamSpec> params() {
        final JsonObject unbound = new JsonObject();
        unbound.addProperty("device", "KEYBOARD");
        unbound.addProperty("code", -1);
        unbound.add("mods", new JsonArray());
        return List.of(
                ParamSpec.of("input", "Key", "Key or mouse button to press", ParamKind.KEYBIND, unbound),
                new ParamSpec("mode", "Press", "How the key is pressed", ParamKind.ENUM, new JsonPrimitive(PressMode.SINGLE.name()),
                        () -> List.of(new Choice("SINGLE", "Single press"), new Choice("DOUBLE_TAP", "Double tap"), new Choice("LONG_HOLD", "Long hold")), null),
                ParamSpec.of("inGuis", "Also fire in inventories", "Fire while a container screen is open (never while typing in a text field)", ParamKind.BOOL, new JsonPrimitive(false))
        );
    }

    @Override
    public List<Port> inputs(MacroNode node) {
        return List.of();
    }

    @Override
    public List<Port> outputs(MacroNode node) {
        return List.of(new Port("main", "", PortKind.OUTPUT));
    }

    @Override
    public String summary(MacroNode node) {
        final Spec spec = spec(node);
        return spec == null ? "Not bound" : spec.chord().displayName();
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        final Spec spec = spec(node);
        if (spec == null) {
            out.add(new NodeIssue(NodeIssue.Severity.ERROR, node.id, "No key bound, this trigger never fires"));
            return;
        }
        if (spec.chord().isPrimaryMouseButton()) {
            out.add(new NodeIssue(spec.inGuis() ? NodeIssue.Severity.ERROR : NodeIssue.Severity.WARNING, node.id,
                    spec.inGuis() ? "Left/right mouse buttons cannot fire in inventories (they would break clicking)"
                            : "Left/right mouse buttons are blocked for attacking/using whenever an action runs"));
        }
    }

}
