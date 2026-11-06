package de.vantrex.skysens.client.features.macro.editor.canvas;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.editor.Highlight;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.ViewportState;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
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

    private enum Mode { IDLE, PAN, SELECT }

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

    /** The (disabled in view-only mode) "+" button in the top right corner; used as an anchor by the tutorial. */
    public int[] addButtonRect() {
        return new int[]{this.x + this.w - ADD_BUTTON - 8, this.y + 8, ADD_BUTTON, ADD_BUTTON};
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
            UiText.drawCentered(g, "Creating macros arrives with the next editor update", this.x + this.w / 2, this.y + this.h / 2 + 4, Theme.TEXT_FAINT);
        } else if (this.shown.nodes.isEmpty()) {
            UiText.drawCentered(g, "This macro has no nodes", this.x + this.w / 2, this.y + this.h / 2 - 4, Theme.TEXT_FAINT);
        }
        this.renderAddButton(g);
        this.controls.render(g, mx, my, this.viewport.zoom, this.x, this.w);
    }

    private void renderAddButton(GuiGraphicsExtractor g) {
        final int[] r = this.addButtonRect();
        final int border = RenderUtil.withAlpha(Theme.POPOVER_BORDER, 0.6f);
        RenderUtil.roundedRectWithBorder(g, r[0], r[1], r[2], r[3], 5, RenderUtil.withAlpha(Theme.POPOVER | 0xFF000000, 0.6f), border);
        final int color = RenderUtil.withAlpha(Theme.TEXT_MUTED, 0.5f);
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
        this.hoveredNode = this.mode == Mode.IDLE && !overControls && this.contains(mx, my) ? this.nodeAt(this.worldX(mx), this.worldY(my)) : null;

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
                if (kind == null) {
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

    private boolean onScreen(NodeLayout layout) {
        final double zoom = this.viewport.zoom;
        final double sx = this.viewport.toScreenX(layout.node.x);
        final double sy = this.viewport.toScreenY(layout.node.y);
        final int margin = 8;
        return sx + layout.w * zoom + margin >= 0 && sx - margin <= this.w && sy + layout.h * zoom + margin >= 0 && sy - margin <= this.h;
    }

    // ------------------------------------------------------------------ input

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

    public boolean mouseClicked(double mx, double my, int button, boolean additive) {
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
        }
        this.lastMouseX = mx;
        this.lastMouseY = my;
        if (button == 2 || (button == 0 && this.spaceDown)) {
            this.mode = Mode.PAN;
            return true;
        }
        if (button != 0) {
            return false;
        }
        final String hit = this.nodeAt(this.worldX(mx), this.worldY(my));
        if (hit != null) {
            if (additive) {
                if (!this.state.selectedNodes().remove(hit)) {
                    this.state.selectedNodes().add(hit);
                }
            } else {
                this.state.selectedNodes().clear();
                this.state.selectedNodes().add(hit);
            }
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

    public boolean mouseDragged(double mx, double my) {
        if (this.mode == Mode.PAN) {
            this.viewport.pan(mx - this.lastMouseX, my - this.lastMouseY);
            this.lastMouseX = mx;
            this.lastMouseY = my;
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

    public boolean mouseReleased() {
        final boolean wasActive = this.mode != Mode.IDLE;
        if (this.mode == Mode.SELECT && !this.selectMoved && this.selectBase.isEmpty()) {
            this.state.selectedNodes().clear();
        }
        this.mode = Mode.IDLE;
        return wasActive;
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
