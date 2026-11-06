package de.vantrex.skysens.client.features.macro.editor.canvas;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.editor.Highlight;
import de.vantrex.skysens.client.features.macro.editor.clipboard.MacroClipboard;
import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.editor.history.AddNodesCommand;
import de.vantrex.skysens.client.features.macro.editor.history.ConnectCommand;
import de.vantrex.skysens.client.features.macro.editor.history.DeleteNodesCommand;
import de.vantrex.skysens.client.features.macro.editor.history.DisconnectCommand;
import de.vantrex.skysens.client.features.macro.editor.history.MoveNodesCommand;
import de.vantrex.skysens.client.features.macro.editor.history.ToggleDisabledCommand;
import de.vantrex.skysens.client.features.macro.editor.picker.NodePicker;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.ViewportState;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.note.StickyNoteNode;
import de.vantrex.skysens.client.features.macro.model.MacroDocument;
import net.minecraft.client.Minecraft;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The canvas of one workflow (view only): grid, wires, nodes, pan, zoom and rectangle selection.
 * <p>
 * Controls (n8n's way): left-drag on empty canvas draws a selection rectangle; pan with middle-drag, Space + left-drag
 * or the scroll wheel (Shift+wheel horizontally); Ctrl+wheel zooms at the cursor; buttons bottom left.
 * All coordinates are in the screen's virtual units; the viewport itself works relative to the canvas' top left corner.
 */
public final class CanvasView {

    private static final int SCROLL_STEP = 40;
    private static final int FIT_PADDING = 40;
    private static final int DRAG_THRESHOLD = 3;
    private static final int ADD_BUTTON = 28;

    private enum Mode { IDLE, PAN, SELECT, DRAG_NODES, WIRE, RESIZE }

    /** A port under the mouse. */
    private record PortHit(String nodeId, String portId, boolean output) {
    }

    /** Everything the picker needs to open: screen anchor, whether only nodes with an input fit, and the request. */
    public record PickerOpen(int anchorX, int anchorY, boolean needInput, NodePicker.Request request) {
    }

    private final EditorState state;
    private final Viewport viewport = new Viewport();
    private final CanvasControls controls = new CanvasControls();

    private int x;
    private int y;
    private int w;
    private int h;

    private @Nullable Workflow shown;
    private Mode mode = Mode.IDLE;
    private boolean spaceDown;
    private double lastMouseX;
    private double lastMouseY;
    private double selectStartX;
    private double selectStartY;
    private double selectEndX;
    private double selectEndY;
    private boolean selectMoved;
    private Set<String> selectBase = Set.of();
    private @Nullable String hoveredNode;
    private Map<String, NodeLayout> layouts = Map.of();
    private int rightInset;
    private @Nullable Consumer<PickerOpen> pickerRequester;

    // node drag
    private Map<String, int[]> dragStart = Map.of();
    private @Nullable String dragAnchor;
    private double dragWorldX;
    private double dragWorldY;
    private double dragScreenX;
    private double dragScreenY;
    private boolean dragMoved;
    private @Nullable String collapseTo;

    // wire drag
    private @Nullable String wireNode;
    private @Nullable String wirePort;
    private boolean wirePickedUp;
    private double wireMouseX;
    private double wireMouseY;
    private @Nullable Connection hoveredWire;
    /** The wire picked up by its input end; it is out of the list while dragged and restored (or replaced) on release. */
    private @Nullable Connection pickedWire;
    private int pickedWireIndex;

    // sticky note resize
    private @Nullable String resizeNode;
    private int[] resizeBefore = new int[4];
    private int resizeStartW;
    private int resizeStartH;
    private double resizeWorldX;
    private double resizeWorldY;

    public CanvasView(EditorState state) {
        this.state = state;
    }

    public void layout(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.controls.layout(x, y, w, h);
    }

    public boolean contains(double mx, double my) {
        return RenderUtil.inside(mx, my, this.x, this.y, this.w, this.h);
    }

    /** The canvas rectangle {x, y, w, h}. */
    public int[] rect() {
        return new int[]{this.x, this.y, this.w, this.h};
    }

    /** The "+" button in the top right corner (left of the details panel when it is open); also the tutorial's anchor. */
    public int[] addButtonRect() {
        return new int[]{this.x + this.w - this.rightInset - ADD_BUTTON - 8, this.y + 8, ADD_BUTTON, ADD_BUTTON};
    }

    /** Width of the details panel covering the right edge, so the "+" button stays visible. */
    public void setRightInset(int rightInset) {
        this.rightInset = rightInset;
    }

    public void setPickerRequester(Consumer<PickerOpen> requester) {
        this.pickerRequester = requester;
    }

    public void setSpaceDown(boolean down) {
        this.spaceDown = down;
    }

    // ------------------------------------------------------------------ workflow and viewport

    /** Follows {@link EditorState#current()}: saves the viewport of the old workflow and loads the new one's. */
    public void sync() {
        if (this.shown == this.state.current()) {
            return;
        }
        this.storeViewport();
        this.shown = this.state.current();
        this.mode = Mode.IDLE;
        if (this.shown != null) {
            this.viewport.load(this.shown.viewport);
            if (Viewport.isDefault(this.shown.viewport) && !this.shown.nodes.isEmpty()) {
                this.fit();
            }
        }
    }

    /** Writes pan and zoom into the shown workflow; marks the state dirty if they changed. */
    public void storeViewport() {
        if (this.shown == null) {
            return;
        }
        final ViewportState saved = this.shown.viewport;
        if (saved.x != this.viewport.panX || saved.y != this.viewport.panY || saved.zoom != this.viewport.zoom) {
            this.viewport.store(saved);
            this.state.markViewportDirty();
        }
    }

    public void fit() {
        if (this.shown == null || this.shown.nodes.isEmpty()) {
            this.viewport.panX = FIT_PADDING;
            this.viewport.panY = FIT_PADDING;
            this.viewport.zoom = 1.0;
            return;
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (MacroNode node : this.shown.nodes) {
            final NodeLayout layout = NodeLayout.of(node);
            minX = Math.min(minX, node.x);
            minY = Math.min(minY, node.y);
            maxX = Math.max(maxX, node.x + layout.w);
            maxY = Math.max(maxY, node.y + layout.h);
        }
        this.viewport.fit(minX, minY, maxX, maxY, this.w, this.h, FIT_PADDING);
    }

    public void resetZoom() {
        this.viewport.zoomTo(this.w / 2.0, this.h / 2.0, 1.0);
    }

    public void zoomStep(int direction) {
        this.viewport.zoomStep(this.w / 2.0, this.h / 2.0, direction);
    }

    /** Screen rectangle {x, y, w, h} of the first If / Switch node, or null if the workflow has none in view. */
    public @Nullable int[] firstLogicNodeRect() {
        if (this.shown == null) {
            return null;
        }
        for (MacroNode node : this.shown.nodes) {
            final NodeLayout layout = NodeLayout.of(node);
            if (!layout.sticky && layout.type.category() == NodeCategory.LOGIC) {
                final int sx = (int) Math.round(this.x + this.viewport.toScreenX(node.x));
                final int sy = (int) Math.round(this.y + this.viewport.toScreenY(node.y));
                final int sw = (int) Math.round(layout.w * this.viewport.zoom);
                final int sh = (int) Math.round(layout.h * this.viewport.zoom);
                // only when it is visible
                if (sx + sw > this.x && sx < this.x + this.w && sy + sh > this.y && sy < this.y + this.h) {
                    return new int[]{sx, sy, sw, sh};
                }
                return null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ rendering

    public void render(GuiGraphicsExtractor g, int mx, int my) {
        this.sync();
        GridRenderer.render(g, this.x, this.y, this.w, this.h, this.viewport);
        RenderUtil.scissor(g, this.x, this.y, this.w, this.h);
        if (this.shown != null) {
            this.renderWorkflow(g, this.shown, mx, my);
        }
        RenderUtil.unscissor(g);

        if (this.shown == null) {
            UiText.drawCentered(g, "No macros yet", this.x + this.w / 2, this.y + this.h / 2 - 10, Theme.TEXT_MUTED, UiText.Weight.SEMIBOLD);
            UiText.drawCentered(g, "Create one with the + in the macro list", this.x + this.w / 2, this.y + this.h / 2 + 4, Theme.TEXT_FAINT);
        } else if (this.shown.nodes.isEmpty()) {
            UiText.drawCentered(g, "This macro has no nodes. Press Tab or + to add one", this.x + this.w / 2, this.y + this.h / 2 - 4, Theme.TEXT_FAINT);
        }
        if (this.shown != null) {
            this.renderAddButton(g, mx, my);
        }
        this.controls.render(g, mx, my, this.viewport.zoom, this.x, this.w);
    }

    private void renderAddButton(GuiGraphicsExtractor g, int mx, int my) {
        final int[] r = this.addButtonRect();
        final boolean hover = RenderUtil.inside(mx, my, r[0], r[1], r[2], r[3]);
        RenderUtil.roundedRectWithBorder(g, r[0], r[1], r[2], r[3], 5, Theme.POPOVER | 0xFF000000, hover ? Theme.ACCENT : Theme.POPOVER_BORDER);
        final int color = hover ? Theme.TEXT : Theme.TEXT_MUTED;
        RenderUtil.rect(g, r[0] + r[2] / 2 - 5, r[1] + r[3] / 2, 11, 1, color);
        RenderUtil.rect(g, r[0] + r[2] / 2, r[1] + r[3] / 2 - 5, 1, 11, color);
    }

    private void renderWorkflow(GuiGraphicsExtractor g, Workflow workflow, int mx, int my) {
        final double zoom = this.viewport.zoom;
        final int originX = this.x + this.viewport.originX();
        final int originY = this.y + this.viewport.originY();

        this.layouts = new LinkedHashMap<>();
        for (MacroNode node : workflow.nodes) {
            this.layouts.put(node.id, NodeLayout.of(node));
        }
        final Highlight highlight = this.state.highlightFor(workflow, System.currentTimeMillis());

        final boolean overControls = this.controls.contains(mx, my);
        final int[] addRect = this.addButtonRect();
        final boolean overAdd = RenderUtil.inside(mx, my, addRect[0], addRect[1], addRect[2], addRect[3]);
        final boolean interactive = this.mode == Mode.IDLE && !overControls && !overAdd && this.contains(mx, my);
        final PortHit hoveredPort = interactive ? this.portAt(mx, my) : null;
        this.hoveredNode = interactive ? this.nodeAt(this.worldX(mx), this.worldY(my)) : null;
        this.hoveredWire = interactive && hoveredPort == null && this.hoveredNode == null ? this.wireAt(mx, my) : null;

        final Map<String, Set<String>> connectedOutputs = new HashMap<>();
        final Set<String> connectedInputs = new HashSet<>();
        for (Connection c : workflow.connections) {
            connectedOutputs.computeIfAbsent(c.from, k -> new HashSet<>()).add(c.port);
            connectedInputs.add(c.to);
        }

        // sticky notes, behind everything
        g.pose().pushMatrix();
        g.pose().translate(originX, originY);
        g.pose().scale((float) zoom, (float) zoom);
        for (NodeLayout layout : this.layouts.values()) {
            if (layout.sticky && this.onScreen(layout)) {
                NodeRenderer.render(g, layout, this.state.selectedNodes().contains(layout.node.id),
                        layout.node.id.equals(this.hoveredNode), null, 0f, false, Set.of());
            }
        }
        g.pose().popMatrix();

        // wires: plain ones first, highlighted ones on top
        final int plain = Theme.TEXT_FAINT | 0xFF000000;
        final int thickness = WireRenderer.thickness(zoom);
        for (int pass = 0; pass < 2; pass++) {
            for (Connection c : workflow.connections) {
                final Highlight.Kind kind = highlight.wire(c);
                if ((kind != null) != (pass == 1)) {
                    continue;
                }
                final NodeLayout from = this.layouts.get(c.from);
                final NodeLayout to = this.layouts.get(c.to);
                if (from == null || to == null || to.inputs.isEmpty()) {
                    continue;
                }
                final int outIndex = from.outputIndex(c.port);
                if (outIndex < 0) {
                    continue;
                }
                final double x1 = this.x + this.viewport.toScreenX(from.node.x + from.w);
                final double y1 = this.y + this.viewport.toScreenY(from.node.y + from.outputY(outIndex));
                final double x2 = this.x + this.viewport.toScreenX(to.node.x);
                final double y2 = this.y + this.viewport.toScreenY(to.node.y + to.inputY());
                if (!WireRenderer.visible(x1, y1, x2, y2, zoom, this.x, this.y, this.w, this.h)) {
                    continue;
                }
                final boolean selectedWire = c == this.state.selectedWire();
                if (selectedWire || c == this.hoveredWire) {
                    WireRenderer.draw(g, x1, y1, x2, y2, zoom, thickness + 1, (selectedWire ? Theme.ACCENT : Theme.TEXT_MUTED) | 0xFF000000);
                } else if (kind == null) {
                    WireRenderer.draw(g, x1, y1, x2, y2, zoom, thickness, plain);
                } else {
                    final int color = RenderUtil.lerpColor(highlight.strength(), plain, NodeRenderer.highlightColor(kind));
                    WireRenderer.draw(g, x1, y1, x2, y2, zoom, thickness + 1, color | 0xFF000000);
                }
            }
        }

        // nodes
        g.pose().pushMatrix();
        g.pose().translate(originX, originY);
        g.pose().scale((float) zoom, (float) zoom);
        for (NodeLayout layout : this.layouts.values()) {
            if (layout.sticky || !this.onScreen(layout)) {
                continue;
            }
            final Highlight.Kind kind = highlight.node(layout.node.id);
            NodeRenderer.render(g, layout, this.state.selectedNodes().contains(layout.node.id),
                    layout.node.id.equals(this.hoveredNode), kind, highlight.strength(),
                    connectedInputs.contains(layout.node.id), connectedOutputs.getOrDefault(layout.node.id, Set.of()));
        }
        g.pose().popMatrix();

        // "x" button on the middle of a hovered or selected wire
        final Connection marked = this.state.selectedWire() != null ? this.state.selectedWire() : this.hoveredWire;
        if (marked != null && this.mode == Mode.IDLE) {
            final double[] mid = this.wireMid(marked);
            if (mid != null) {
                final int cx = (int) Math.round(mid[0]);
                final int cy = (int) Math.round(mid[1]);
                final boolean over = Math.hypot(mx - mid[0], my - mid[1]) <= 8;
                final int color = over ? Theme.DANGER : Theme.TEXT_MUTED;
                RenderUtil.circle(g, cx, cy, 8, over ? Theme.DANGER : Theme.POPOVER_BORDER);
                RenderUtil.circle(g, cx, cy, 7, Theme.POPOVER | 0xFF000000);
                for (int i = -2; i <= 2; i++) {
                    RenderUtil.rect(g, cx + i, cy + i, 1, 1, color);
                    RenderUtil.rect(g, cx + i, cy - i, 1, 1, color);
                }
            }
        }

        // port feedback and the wire being dragged
        if (hoveredPort != null) {
            this.portRing(g, hoveredPort, Theme.ACCENT);
        }
        if (this.mode == Mode.WIRE && this.wireNode != null) {
            final NodeLayout from = this.layouts.get(this.wireNode);
            if (from != null) {
                final int idx = from.outputIndex(this.wirePort);
                if (idx >= 0) {
                    final double x1 = this.x + this.viewport.toScreenX(from.node.x + from.w);
                    final double y1 = this.y + this.viewport.toScreenY(from.node.y + from.outputY(idx));
                    WireRenderer.draw(g, x1, y1, this.wireMouseX, this.wireMouseY, zoom, thickness + 1, Theme.ACCENT | 0xFF000000);
                }
            }
            final String target = this.wireTarget(this.wireMouseX, this.wireMouseY);
            if (target != null) {
                this.portRing(g, new PortHit(target, "main", false), Theme.ACCENT);
            }
        }

        if (this.mode == Mode.SELECT && this.selectMoved) {
            final int sx1 = (int) Math.round(this.x + this.viewport.toScreenX(Math.min(this.selectStartX, this.selectEndX)));
            final int sy1 = (int) Math.round(this.y + this.viewport.toScreenY(Math.min(this.selectStartY, this.selectEndY)));
            final int sx2 = (int) Math.round(this.x + this.viewport.toScreenX(Math.max(this.selectStartX, this.selectEndX)));
            final int sy2 = (int) Math.round(this.y + this.viewport.toScreenY(Math.max(this.selectStartY, this.selectEndY)));
            RenderUtil.rect(g, sx1, sy1, sx2 - sx1, sy2 - sy1, RenderUtil.withAlpha(Theme.ACCENT, 0.15f));
            final int border = RenderUtil.withAlpha(Theme.ACCENT, 0.9f);
            RenderUtil.rect(g, sx1, sy1, sx2 - sx1, 1, border);
            RenderUtil.rect(g, sx1, sy2 - 1, sx2 - sx1, 1, border);
            RenderUtil.rect(g, sx1, sy1, 1, sy2 - sy1, border);
            RenderUtil.rect(g, sx2 - 1, sy1, 1, sy2 - sy1, border);
        }
    }

    private void portRing(GuiGraphicsExtractor g, PortHit hit, int color) {
        final NodeLayout layout = this.layouts.get(hit.nodeId());
        if (layout == null) {
            return;
        }
        final double[] p = this.portScreen(layout, hit);
        if (p == null) {
            return;
        }
        final int r = (int) Math.round((NodeLayout.PORT_RADIUS + 3) * this.viewport.zoom);
        RenderUtil.roundedRing(g, (int) Math.round(p[0]) - r, (int) Math.round(p[1]) - r, 2 * r, 2 * r, r, color);
    }

    private @Nullable double[] portScreen(NodeLayout layout, PortHit hit) {
        if (hit.output()) {
            final int idx = layout.outputIndex(hit.portId());
            return idx < 0 ? null : new double[]{this.x + this.viewport.toScreenX(layout.node.x + layout.w),
                    this.y + this.viewport.toScreenY(layout.node.y + layout.outputY(idx))};
        }
        return layout.inputs.isEmpty() ? null : new double[]{this.x + this.viewport.toScreenX(layout.node.x),
                this.y + this.viewport.toScreenY(layout.node.y + layout.inputY())};
    }

    private boolean onScreen(NodeLayout layout) {
        final double zoom = this.viewport.zoom;
        final double sx = this.viewport.toScreenX(layout.node.x);
        final double sy = this.viewport.toScreenY(layout.node.y);
        final int margin = 8;
        return sx + layout.w * zoom + margin >= 0 && sx - margin <= this.w && sy + layout.h * zoom + margin >= 0 && sy - margin <= this.h;
    }

    // ------------------------------------------------------------------ hit testing

    private double worldX(double mx) {
        return this.viewport.toWorldX(mx - this.x);
    }

    private double worldY(double my) {
        return this.viewport.toWorldY(my - this.y);
    }

    /** The topmost node at the world point; cards win over sticky notes. */
    private @Nullable String nodeAt(double wx, double wy) {
        final List<NodeLayout> all = new ArrayList<>(this.layouts.values());
        for (int i = all.size() - 1; i >= 0; i--) {
            if (!all.get(i).sticky && all.get(i).contains(wx, wy)) {
                return all.get(i).node.id;
            }
        }
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).sticky && all.get(i).contains(wx, wy)) {
                return all.get(i).node.id;
            }
        }
        return null;
    }

    private @Nullable PortHit portAt(double mx, double my) {
        final double reach = Math.max(8, (NodeLayout.PORT_RADIUS + 4) * this.viewport.zoom);
        final List<NodeLayout> all = new ArrayList<>(this.layouts.values());
        for (int n = all.size() - 1; n >= 0; n--) {
            final NodeLayout layout = all.get(n);
            if (layout.sticky) {
                continue;
            }
            for (Port port : layout.outputs) {
                final double[] p = this.portScreen(layout, new PortHit(layout.node.id, port.id(), true));
                if (p != null && Math.hypot(mx - p[0], my - p[1]) <= reach) {
                    return new PortHit(layout.node.id, port.id(), true);
                }
            }
            if (!layout.inputs.isEmpty()) {
                final PortHit hit = new PortHit(layout.node.id, layout.inputs.get(0).id(), false);
                final double[] p = this.portScreen(layout, hit);
                if (p != null && Math.hypot(mx - p[0], my - p[1]) <= reach) {
                    return hit;
                }
            }
        }
        return null;
    }

    private @Nullable double[] wireEnds(Connection c) {
        final NodeLayout from = this.layouts.get(c.from);
        final NodeLayout to = this.layouts.get(c.to);
        if (from == null || to == null || to.inputs.isEmpty()) {
            return null;
        }
        final int idx = from.outputIndex(c.port);
        if (idx < 0) {
            return null;
        }
        return new double[]{this.x + this.viewport.toScreenX(from.node.x + from.w), this.y + this.viewport.toScreenY(from.node.y + from.outputY(idx)),
                this.x + this.viewport.toScreenX(to.node.x), this.y + this.viewport.toScreenY(to.node.y + to.inputY())};
    }

    private @Nullable double[] wireMid(Connection c) {
        final double[] e = this.wireEnds(c);
        return e == null ? null : WireRenderer.pointAt(e[0], e[1], e[2], e[3], this.viewport.zoom, 0.5);
    }

    private @Nullable Connection wireAt(double mx, double my) {
        if (this.shown == null) {
            return null;
        }
        Connection best = null;
        double bestDistance = 6;
        for (Connection c : this.shown.connections) {
            final double[] e = this.wireEnds(c);
            if (e == null || !WireRenderer.visible(e[0], e[1], e[2], e[3], this.viewport.zoom, this.x, this.y, this.w, this.h)) {
                continue;
            }
            final double d = WireRenderer.distance(e[0], e[1], e[2], e[3], this.viewport.zoom, mx, my);
            if (d <= bestDistance) {
                bestDistance = d;
                best = c;
            }
        }
        return best;
    }

    /** The node a dragged wire would connect to at this screen point: an input port or the node body. */
    private @Nullable String wireTarget(double mx, double my) {
        final PortHit port = this.portAt(mx, my);
        String id = port != null && !port.output() ? port.nodeId() : null;
        if (id == null) {
            final String body = this.nodeAt(this.worldX(mx), this.worldY(my));
            final NodeLayout layout = body == null ? null : this.layouts.get(body);
            if (layout != null && !layout.sticky && !layout.inputs.isEmpty()) {
                id = body;
            }
        }
        return id == null || id.equals(this.wireNode) ? null : id;
    }

    private static boolean altDown() {
        final var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LALT) || InputConstants.isKeyDown(window, InputConstants.KEY_RALT);
    }

    public boolean mouseClicked(double mx, double my, int button, boolean additive, boolean doubleClick) {
        this.sync();
        if (!this.contains(mx, my)) {
            return false;
        }
        if (button == 0) {
            final CanvasControls.Action action = this.controls.hit(mx, my);
            if (action != null) {
                this.perform(action);
                return true;
            }
            if (this.controls.contains(mx, my)) {
                return true;
            }
            final int[] add = this.addButtonRect();
            if (this.shown != null && RenderUtil.inside(mx, my, add[0], add[1], add[2], add[3])) {
                this.openPicker(add[0] + add[2] - 260, add[1] + add[3] + 4, null);
                return true;
            }
        }
        this.lastMouseX = mx;
        this.lastMouseY = my;
        if (button == 2 || (button == 0 && this.spaceDown)) {
            this.mode = Mode.PAN;
            return true;
        }
        if (button != 0 || this.shown == null) {
            return false;
        }

        // wires: the x button, ports
        final Connection marked = this.state.selectedWire() != null ? this.state.selectedWire() : this.hoveredWire;
        if (marked != null) {
            final double[] mid = this.wireMid(marked);
            if (mid != null && Math.hypot(mx - mid[0], my - mid[1]) <= 8) {
                this.disconnect(marked);
                return true;
            }
        }
        final NodeLayout handle = this.resizeHandleAt(mx, my);
        if (handle != null) {
            this.mode = Mode.RESIZE;
            this.resizeNode = handle.node.id;
            this.resizeBefore = MoveNodesCommand.bounds(handle.node);
            this.resizeStartW = handle.w;
            this.resizeStartH = handle.h;
            this.resizeWorldX = this.worldX(mx);
            this.resizeWorldY = this.worldY(my);
            this.state.selectedNodes().clear();
            this.state.selectedNodes().add(handle.node.id);
            this.state.setSelectedWire(null);
            return true;
        }
        final PortHit port = this.portAt(mx, my);
        if (port != null) {
            if (port.output()) {
                this.startWire(port.nodeId(), port.portId(), false, mx, my);
                return true;
            }
            // pick the wire up by its input end: it disconnects and can be dropped somewhere else
            Connection incoming = null;
            for (Connection c : this.shown.connections) {
                if (c.to.equals(port.nodeId())) {
                    incoming = c;
                }
            }
            if (incoming != null) {
                this.pickedWireIndex = this.shown.connections.indexOf(incoming);
                this.shown.connections.remove(incoming);
                this.pickedWire = incoming;
                this.startWire(incoming.from, incoming.port, true, mx, my);
                return true;
            }
        }

        final String hit = this.nodeAt(this.worldX(mx), this.worldY(my));
        if (hit != null) {
            this.state.setSelectedWire(null);
            final Set<String> selection = this.state.selectedNodes();
            this.collapseTo = null;
            if (doubleClick) {
                selection.clear();
                selection.add(hit);
                this.state.setDetailsNode(hit);
                return true;
            }
            if (additive) {
                if (!selection.remove(hit)) {
                    selection.add(hit);
                } else {
                    return true;
                }
            } else if (!selection.contains(hit)) {
                selection.clear();
                selection.add(hit);
            } else if (selection.size() > 1) {
                this.collapseTo = hit;
            }
            this.beginNodeDrag(hit, mx, my);
            return true;
        }

        final Connection wire = this.wireAt(mx, my);
        if (wire != null) {
            this.state.selectedNodes().clear();
            this.state.setSelectedWire(wire);
            return true;
        }

        this.mode = Mode.SELECT;
        this.selectStartX = this.worldX(mx);
        this.selectStartY = this.worldY(my);
        this.selectEndX = this.selectStartX;
        this.selectEndY = this.selectStartY;
        this.selectMoved = false;
        this.selectBase = additive ? new LinkedHashSet<>(this.state.selectedNodes()) : Set.of();
        return true;
    }

    private void startWire(String nodeId, String portId, boolean pickedUp, double mx, double my) {
        this.mode = Mode.WIRE;
        this.wireNode = nodeId;
        this.wirePort = portId;
        this.wirePickedUp = pickedUp;
        this.wireMouseX = mx;
        this.wireMouseY = my;
        this.state.setSelectedWire(null);
    }

    private void beginNodeDrag(String anchor, double mx, double my) {
        this.mode = Mode.DRAG_NODES;
        this.dragAnchor = anchor;
        this.dragMoved = false;
        this.dragWorldX = this.worldX(mx);
        this.dragWorldY = this.worldY(my);
        this.dragScreenX = mx;
        this.dragScreenY = my;
        this.dragStart = new HashMap<>();
        if (this.shown != null) {
            for (MacroNode node : this.shown.nodes) {
                if (this.state.selectedNodes().contains(node.id)) {
                    this.dragStart.put(node.id, new int[]{node.x, node.y});
                }
            }
        }
    }

    public boolean mouseDragged(double mx, double my) {
        if (this.mode == Mode.PAN) {
            this.viewport.pan(mx - this.lastMouseX, my - this.lastMouseY);
            this.lastMouseX = mx;
            this.lastMouseY = my;
            return true;
        }
        if (this.mode == Mode.WIRE) {
            this.wireMouseX = mx;
            this.wireMouseY = my;
            return true;
        }
        if (this.mode == Mode.RESIZE && this.shown != null) {
            final MacroNode node = GraphEdit.find(this.shown, this.resizeNode);
            if (node != null) {
                double w = this.resizeStartW + this.worldX(mx) - this.resizeWorldX;
                double h = this.resizeStartH + this.worldY(my) - this.resizeWorldY;
                if (!altDown()) {
                    w = Math.round(w / 10.0) * 10;
                    h = Math.round(h / 10.0) * 10;
                }
                node.w = (int) Math.max(StickyNoteNode.MIN_W, w);
                node.h = (int) Math.max(StickyNoteNode.MIN_H, h);
            }
            return true;
        }
        if (this.mode == Mode.DRAG_NODES && this.shown != null) {
            if (!this.dragMoved && Math.hypot(mx - this.dragScreenX, my - this.dragScreenY) < DRAG_THRESHOLD) {
                return true;
            }
            this.dragMoved = true;
            double dx = this.worldX(mx) - this.dragWorldX;
            double dy = this.worldY(my) - this.dragWorldY;
            final int[] anchor = this.dragStart.get(this.dragAnchor);
            if (anchor != null && !altDown()) {
                dx = Math.round((anchor[0] + dx) / 10.0) * 10 - anchor[0];
                dy = Math.round((anchor[1] + dy) / 10.0) * 10 - anchor[1];
            }
            for (MacroNode node : this.shown.nodes) {
                final int[] start = this.dragStart.get(node.id);
                if (start != null) {
                    node.x = (int) Math.round(start[0] + dx);
                    node.y = (int) Math.round(start[1] + dy);
                }
            }
            this.state.markDirty();
            return true;
        }
        if (this.mode == Mode.SELECT) {
            this.selectEndX = this.worldX(mx);
            this.selectEndY = this.worldY(my);
            if (!this.selectMoved
                    && Math.hypot((this.selectEndX - this.selectStartX) * this.viewport.zoom, (this.selectEndY - this.selectStartY) * this.viewport.zoom) >= DRAG_THRESHOLD) {
                this.selectMoved = true;
            }
            if (this.selectMoved) {
                final Set<String> selection = this.state.selectedNodes();
                selection.clear();
                selection.addAll(this.selectBase);
                final double minX = Math.min(this.selectStartX, this.selectEndX);
                final double maxX = Math.max(this.selectStartX, this.selectEndX);
                final double minY = Math.min(this.selectStartY, this.selectEndY);
                final double maxY = Math.max(this.selectStartY, this.selectEndY);
                for (NodeLayout layout : this.layouts.values()) {
                    if (layout.intersects(minX, minY, maxX, maxY)) {
                        selection.add(layout.node.id);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my) {
        final boolean wasActive = this.mode != Mode.IDLE;
        final Mode finished = this.mode;
        this.mode = Mode.IDLE;
        if (finished == Mode.SELECT && !this.selectMoved && this.selectBase.isEmpty()) {
            this.state.selectedNodes().clear();
            this.state.setSelectedWire(null);
            this.state.setDetailsNode(null);
        } else if (finished == Mode.DRAG_NODES && !this.dragMoved && this.collapseTo != null) {
            this.state.selectedNodes().clear();
            this.state.selectedNodes().add(this.collapseTo);
        } else if (finished == Mode.DRAG_NODES && this.dragMoved) {
            this.recordMove();
        } else if (finished == Mode.RESIZE) {
            this.recordResize();
        } else if (finished == Mode.WIRE) {
            this.finishWire(mx, my);
        }
        this.collapseTo = null;
        return wasActive;
    }

    private void finishWire(double mx, double my) {
        final Workflow workflow = this.shown;
        final String fromNode = this.wireNode;
        final String fromPort = this.wirePort;
        final Connection picked = this.pickedWire;
        final int pickedIndex = this.pickedWireIndex;
        this.wireNode = null;
        this.pickedWire = null;
        if (workflow == null || fromNode == null || fromPort == null) {
            return;
        }
        final String target = this.wireTarget(mx, my);
        if (target != null) {
            if (picked != null && target.equals(picked.to)) {
                this.restore(workflow, picked, pickedIndex); // dropped back where it was
                return;
            }
            final GraphEdit.ConnectResult result = GraphEdit.check(workflow, fromNode, fromPort, target);
            if (picked != null) {
                this.restore(workflow, picked, pickedIndex); // the new wire replaces it, as one undo step
            }
            if (result == GraphEdit.ConnectResult.OK) {
                this.state.history().execute(new ConnectCommand(workflow, new Connection(fromNode, fromPort, target)));
            } else {
                this.state.toast(GraphEdit.describe(result), true);
            }
            return;
        }
        if (!this.contains(mx, my) || this.controls.contains(mx, my)) {
            if (picked != null) {
                this.restore(workflow, picked, pickedIndex);
            }
            return;
        }
        if (picked != null) {
            // dropped on empty canvas: the wire stays disconnected
            this.restore(workflow, picked, pickedIndex);
            this.state.history().execute(new DisconnectCommand(workflow, picked, pickedIndex));
            return;
        }
        // new wire dropped on empty canvas: pick a node and connect it
        final NodePicker.Request request = new NodePicker.Request(this.worldX(mx), this.worldY(my) - NodeLayout.HEIGHT / 2.0, fromNode, fromPort);
        if (this.pickerRequester != null) {
            this.pickerRequester.accept(new PickerOpen((int) mx + 8, (int) my - 20, true, request));
        }
    }

    private void restore(Workflow workflow, Connection wire, int index) {
        workflow.connections.add(Math.max(0, Math.min(index, workflow.connections.size())), wire);
    }

    private void disconnect(Connection wire) {
        if (this.shown != null) {
            this.state.history().execute(new DisconnectCommand(this.shown, wire, this.shown.connections.indexOf(wire)));
        }
    }

    /** One undo step for a whole drag. */
    private void recordMove() {
        if (this.shown == null) {
            return;
        }
        final Map<String, int[]> before = new HashMap<>();
        final Map<String, int[]> after = new HashMap<>();
        boolean changed = false;
        for (MacroNode node : this.shown.nodes) {
            final int[] start = this.dragStart.get(node.id);
            if (start != null) {
                before.put(node.id, new int[]{start[0], start[1], node.w, node.h});
                after.put(node.id, MoveNodesCommand.bounds(node));
                changed |= start[0] != node.x || start[1] != node.y;
            }
        }
        if (changed) {
            this.state.history().record(new MoveNodesCommand(this.shown, before, after, before.size() == 1 ? "Move node" : "Move nodes"));
        }
    }

    private void recordResize() {
        final MacroNode node = this.shown == null ? null : GraphEdit.find(this.shown, this.resizeNode);
        this.resizeNode = null;
        if (node == null) {
            return;
        }
        final int[] after = MoveNodesCommand.bounds(node);
        if (java.util.Arrays.equals(after, this.resizeBefore)) {
            return;
        }
        this.state.history().record(new MoveNodesCommand(this.shown, Map.of(node.id, this.resizeBefore), Map.of(node.id, after), "Resize note"));
    }

    /** The sticky note whose bottom right corner handle is under the mouse. */
    private @Nullable NodeLayout resizeHandleAt(double mx, double my) {
        final double size = Math.max(10, StickyNoteRenderer.HANDLE * this.viewport.zoom);
        final List<NodeLayout> all = new ArrayList<>(this.layouts.values());
        for (int i = all.size() - 1; i >= 0; i--) {
            final NodeLayout layout = all.get(i);
            if (!layout.sticky) {
                continue;
            }
            final double right = this.x + this.viewport.toScreenX(layout.node.x + layout.w);
            final double bottom = this.y + this.viewport.toScreenY(layout.node.y + layout.h);
            if (mx <= right + 2 && mx >= right - size && my <= bottom + 2 && my >= bottom - size) {
                return layout;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ editing commands

    /**
     * Opens the node picker (Tab, the "+" button). With exactly one node selected that has a free output, the new node
     * is placed to its right and wired from it; otherwise it goes to the cursor (or the canvas centre).
     */
    public void openPicker(int anchorX, int anchorY, @Nullable double[] cursor) {
        if (this.shown == null || this.pickerRequester == null) {
            return;
        }
        NodePicker.Request request = null;
        if (this.state.selectedNodes().size() == 1) {
            final NodeLayout layout = this.layouts.get(this.state.selectedNodes().iterator().next());
            if (layout != null && !layout.sticky) {
                for (Port out : layout.outputs) {
                    final boolean used = this.shown.connections.stream().anyMatch(c -> c.from.equals(layout.node.id) && c.port.equals(out.id()));
                    if (!used) {
                        final double wy = layout.node.y + layout.outputY(layout.outputIndex(out.id())) - NodeLayout.HEIGHT / 2.0;
                        request = new NodePicker.Request(layout.node.x + layout.w + 60, wy, layout.node.id, out.id());
                        break;
                    }
                }
            }
        }
        if (request == null) {
            final double wx = cursor != null ? this.worldX(cursor[0]) : this.viewport.toWorldX(this.w / 2.0) - NodeLayout.WIDTH / 2.0;
            final double wy = cursor != null ? this.worldY(cursor[1]) : this.viewport.toWorldY(this.h / 2.0) - NodeLayout.HEIGHT / 2.0;
            request = new NodePicker.Request(wx, wy, null, null);
        }
        this.pickerRequester.accept(new PickerOpen(anchorX, anchorY, request.fromNode() != null, request));
    }

    /** Creates the node the picker chose, selects it and wires it if the request has a source. One undo step. */
    public void place(NodePicker.Entry entry, NodePicker.Request request) {
        if (this.shown == null) {
            return;
        }
        final int px = (int) Math.round(request.worldX() / 10.0) * 10;
        final int py = (int) Math.round(request.worldY() / 10.0) * 10;
        final MacroNode node = entry.condition() != null
                ? GraphEdit.createConditionNode(entry.condition(), px, py)
                : GraphEdit.createNode(entry.node(), px, py);
        final List<Connection> wires = new ArrayList<>();
        if (request.fromNode() != null && request.fromPort() != null) {
            final MacroNode from = GraphEdit.find(this.shown, request.fromNode());
            final boolean portExists = from != null && NodeTypes.resolve(from.type).outputs(from).stream().anyMatch(p -> p.id().equals(request.fromPort()));
            if (portExists && !NodeTypes.resolve(node.type).inputs(node).isEmpty()) {
                wires.add(new Connection(request.fromNode(), request.fromPort(), node.id));
            } else {
                this.state.toast(GraphEdit.describe(GraphEdit.ConnectResult.NO_PORT), true);
            }
        }
        this.state.history().execute(new AddNodesCommand(this.shown, List.of(node), wires, "Add node"));
    }

    /** Delete / Backspace: the selected wire, or else the selected nodes with their wires. */
    public boolean deleteSelection() {
        if (this.shown == null) {
            return false;
        }
        final Connection wire = this.state.selectedWire();
        if (wire != null) {
            this.disconnect(wire);
            return true;
        }
        final Set<String> ids = new LinkedHashSet<>(this.state.selectedNodes());
        if (ids.isEmpty()) {
            return false;
        }
        this.state.history().execute(new DeleteNodesCommand(this.shown, ids));
        this.layouts = Map.of();
        return true;
    }

    /** Whether no drag or wire gesture is running (undo is refused mid-gesture). */
    public boolean isIdle() {
        return this.mode == Mode.IDLE;
    }

    /** Ctrl+C. */
    public boolean copySelection() {
        if (this.shown == null || this.state.selectedNodes().isEmpty()) {
            return false;
        }
        MacroClipboard.copyNodes(this.shown, this.state.selectedNodes());
        final int n = this.state.selectedNodes().size();
        this.state.toast("Copied " + n + (n == 1 ? " node" : " nodes"), false);
        return true;
    }

    /** Ctrl+X. */
    public boolean cutSelection() {
        if (!this.copySelection()) {
            return false;
        }
        this.state.setSelectedWire(null);
        return this.deleteSelection();
    }

    /** Ctrl+V: pastes at the cursor (or the canvas centre when the cursor is elsewhere). Failures become toasts. */
    public boolean paste(double mx, double my) {
        if (this.shown == null) {
            return false;
        }
        final MacroDocument doc;
        try {
            doc = MacroClipboard.read();
        } catch (Exception e) {
            this.state.toast(MacroClipboard.describeFailure(e), true);
            return true;
        }
        final boolean inside = this.contains(mx, my);
        final double wx = inside ? this.worldX(mx) : this.viewport.toWorldX(this.w / 2.0);
        final double wy = inside ? this.worldY(my) : this.viewport.toWorldY(this.h / 2.0);
        final MacroClipboard.Pasted pasted = MacroClipboard.nodesFor(doc, wx, wy);
        if (pasted.nodes().isEmpty()) {
            this.state.toast("The clipboard has no nodes to paste", true);
            return true;
        }
        this.state.history().execute(new AddNodesCommand(this.shown, pasted.nodes(), pasted.connections(), "Paste"));
        return true;
    }

    /** Ctrl+D: copies the selection next to itself. */
    public boolean duplicateSelection() {
        if (this.shown == null || this.state.selectedNodes().isEmpty()) {
            return false;
        }
        final MacroClipboard.Pasted pasted = MacroClipboard.duplicateNodes(this.shown, this.state.selectedNodes());
        if (pasted.nodes().isEmpty()) {
            return false;
        }
        this.state.history().execute(new AddNodesCommand(this.shown, pasted.nodes(), pasted.connections(), "Duplicate"));
        return true;
    }

    /** D: disables the selected nodes, or enables them when they all are disabled already. Sticky notes are skipped. */
    public boolean toggleDisabledOnSelection() {
        if (this.shown == null) {
            return false;
        }
        final List<MacroNode> nodes = new ArrayList<>();
        for (MacroNode node : this.shown.nodes) {
            if (this.state.selectedNodes().contains(node.id) && !NodeLayout.of(node).sticky) {
                nodes.add(node);
            }
        }
        final ToggleDisabledCommand command = new ToggleDisabledCommand(this.shown, nodes);
        if (command.isEmpty()) {
            return false;
        }
        this.state.history().execute(command);
        return true;
    }

    public void selectAll() {
        if (this.shown != null) {
            this.state.selectedNodes().clear();
            this.shown.nodes.forEach(n -> this.state.selectedNodes().add(n.id));
        }
    }

    /** Enter: opens the details of the single selected node. */
    public boolean openDetailsOfSelection() {
        if (this.state.selectedNodes().size() == 1) {
            this.state.setDetailsNode(this.state.selectedNodes().iterator().next());
            return true;
        }
        return false;
    }

    /** Wheel: pans (Shift: horizontally) or, with Ctrl, zooms at the cursor. */
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY, boolean ctrl, boolean shift) {
        if (!this.contains(mx, my)) {
            return false;
        }
        if (ctrl) {
            if (scrollY != 0) {
                this.viewport.zoomStep(mx - this.x, my - this.y, scrollY > 0 ? 1 : -1);
            }
            return true;
        }
        // some platforms report Shift+wheel on the X axis
        final double horizontal = scrollX != 0 ? scrollX : (shift ? scrollY : 0);
        final double vertical = shift ? 0 : scrollY;
        this.viewport.pan(horizontal * SCROLL_STEP, vertical * SCROLL_STEP);
        return true;
    }

    private void perform(CanvasControls.Action action) {
        switch (action) {
            case ZOOM_OUT -> this.zoomStep(-1);
            case ZOOM_IN -> this.zoomStep(1);
            case FIT -> this.fit();
            case RESET -> this.resetZoom();
        }
    }

}
