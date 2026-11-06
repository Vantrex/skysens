package de.vantrex.skysens.client.features.macro.editor.canvas;

import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.note.StickyNoteNode;

import java.util.List;

/** Size and port positions of a node, in world units relative to the node's top left corner. */
public final class NodeLayout {

    public static final int WIDTH = 150;
    public static final int WIDTH_LABELLED = 170;
    public static final int HEIGHT = 44;
    public static final int ROW_HEIGHT = 18;
    public static final int PORT_RADIUS = 4;

    public final MacroNode node;
    public final NodeType type;
    public final boolean sticky;
    public final int w;
    public final int h;
    public final List<Port> inputs;
    public final List<Port> outputs;
    /** True when at least one output has a label (If, Switch): labels are drawn inside the node, next to the ports. */
    public final boolean labelled;

    private NodeLayout(MacroNode node) {
        this.node = node;
        this.type = NodeTypes.resolve(node.type);
        this.sticky = this.type.category() == NodeCategory.NOTE;
        if (this.sticky) {
            this.inputs = List.of();
            this.outputs = List.of();
            this.labelled = false;
            this.w = node.w > 0 ? node.w : StickyNoteNode.DEFAULT_W;
            this.h = node.h > 0 ? node.h : StickyNoteNode.DEFAULT_H;
            return;
        }
        this.inputs = this.type.inputs(node);
        this.outputs = this.type.outputs(node);
        this.labelled = this.outputs.stream().anyMatch(port -> !port.label().isEmpty());
        this.w = this.labelled ? WIDTH_LABELLED : WIDTH;
        this.h = this.outputs.size() >= 2 ? 20 + ROW_HEIGHT * this.outputs.size() : HEIGHT;
    }

    public static NodeLayout of(MacroNode node) {
        return new NodeLayout(node);
    }

    /** Y of an output port relative to the node's top. */
    public int outputY(int index) {
        return this.outputs.size() >= 2 ? 10 + ROW_HEIGHT / 2 + ROW_HEIGHT * index : this.h / 2;
    }

    public int inputY() {
        return this.h / 2;
    }

    public int outputIndex(String portId) {
        for (int i = 0; i < this.outputs.size(); i++) {
            if (this.outputs.get(i).id().equals(portId)) {
                return i;
            }
        }
        return -1;
    }

    public int inputIndex(String portId) {
        for (int i = 0; i < this.inputs.size(); i++) {
            if (this.inputs.get(i).id().equals(portId)) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(double worldX, double worldY) {
        return worldX >= this.node.x && worldX < this.node.x + this.w && worldY >= this.node.y && worldY < this.node.y + this.h;
    }

    public boolean intersects(double minX, double minY, double maxX, double maxY) {
        return this.node.x < maxX && this.node.x + this.w > minX && this.node.y < maxY && this.node.y + this.h > minY;
    }

}
