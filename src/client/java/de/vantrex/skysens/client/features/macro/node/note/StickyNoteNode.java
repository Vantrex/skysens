package de.vantrex.skysens.client.features.macro.node.note;

import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.Arrays;
import java.util.List;

/** A note on the canvas: no ports, never run. Size comes from the node's w and h. */
public final class StickyNoteNode implements NodeType {

    public static final String ID = "note.sticky";
    public static final int DEFAULT_W = 200;
    public static final int DEFAULT_H = 100;
    public static final int MIN_W = 80;
    public static final int MIN_H = 50;

    /** Persisted by name. Fill colours are ARGB. */
    public enum Color {
        YELLOW("Yellow", 0xFFE8C547), BLUE("Blue", 0xFF6FA8DC), GREEN("Green", 0xFF7CC47F), PINK("Pink", 0xFFE79AB5), GRAY("Gray", 0xFFB0B4BA);

        public final String label;
        public final int argb;

        Color(String label, int argb) {
            this.label = label;
            this.argb = argb;
        }
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Sticky Note";
    }

    @Override
    public NodeCategory category() {
        return NodeCategory.NOTE;
    }

    @Override
    public String icon() {
        return "N";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                ParamSpec.of("text", "Text", "Your note", ParamKind.TEXT_MULTILINE, new JsonPrimitive("")),
                new ParamSpec("color", "Colour", "Fill colour", ParamKind.ENUM, new JsonPrimitive(Color.YELLOW.name()),
                        () -> Arrays.stream(Color.values()).map(c -> new Choice(c.name(), c.label)).toList(), null));
    }

    @Override
    public List<Port> inputs(MacroNode node) {
        return List.of();
    }

    @Override
    public List<Port> outputs(MacroNode node) {
        return List.of();
    }

    @Override
    public String summary(MacroNode node) {
        return Params.getString(node.params, "text", "");
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
    }

    public static Color colorOf(MacroNode node) {
        return Params.getEnum(node.params, "color", Color.class, Color.YELLOW);
    }

}
