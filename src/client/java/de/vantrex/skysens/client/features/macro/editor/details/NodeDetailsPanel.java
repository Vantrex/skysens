package de.vantrex.skysens.client.features.macro.editor.details;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.MacroService;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.editor.canvas.NodeRenderer;
import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.editor.field.FieldContext;
import de.vantrex.skysens.client.features.macro.editor.field.FieldUi;
import de.vantrex.skysens.client.features.macro.editor.field.ParamFieldList;
import de.vantrex.skysens.client.features.macro.editor.history.EditNodeCommand;
import de.vantrex.skysens.client.features.macro.editor.history.ToggleDisabledCommand;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.ui.SearchField;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The right-hand settings panel of a node (n8n's node details view), opened by double-clicking a node. Its fields are
 * generated from the node type's param specs; the validation issues of the node are listed at the bottom.
 */
public final class NodeDetailsPanel {

    public static final int WIDTH = 300;
    private static final int HEADER_H = 34;
    private static final int PAD = 12;
    private static final long ISSUE_REFRESH_MS = 250L;

    private final EditorState state;
    private final SearchField nameField = new SearchField(this::onName).noHint().maxLength(40);

    private int x;
    private int y;
    private int w = WIDTH;
    private int h;
    private float scroll;
    private int contentHeight;

    private @Nullable String builtFor;
    private @Nullable Workflow builtWorkflow;
    private @Nullable ParamFieldList fields;
    private boolean initialising;
    private List<NodeIssue> issues = List.of();
    private long issuesTime;
    private boolean issuesStale = true;
    private int builtEpoch = -1;
    private EditNodeCommand.@Nullable Snapshot snapshot;
    private int[] disabledRow = new int[4];

    public NodeDetailsPanel(EditorState state) {
        this.state = state;
    }

    public void layout(int canvasX, int canvasY, int canvasW, int canvasH) {
        this.w = Math.min(WIDTH, Math.max(120, canvasW - 40));
        this.x = canvasX + canvasW - this.w;
        this.y = canvasY;
        this.h = canvasH;
    }

    public int width() {
        return this.w;
    }

    public int[] rect() {
        return new int[]{this.x, this.y, this.w, this.h};
    }

    private @Nullable MacroNode node() {
        final Workflow workflow = this.state.current();
        return workflow == null ? null : GraphEdit.find(workflow, this.state.detailsNode());
    }

    public boolean isOpen() {
        return this.node() != null;
    }

    public boolean contains(double mx, double my) {
        return this.isOpen() && RenderUtil.inside(mx, my, this.x, this.y, this.w, this.h);
    }

    public void close() {
        this.blur();
        this.state.setDetailsNode(null);
        this.builtFor = null;
    }

    public void blur() {
        this.nameField.setFocused(false);
        if (this.fields != null) {
            this.fields.blur();
        }
    }

    public boolean isTextFocused() {
        return this.nameField.isFocused() || (this.fields != null && this.fields.isTextFocused());
    }

    private void onName(String text) {
        final MacroNode node = this.node();
        if (this.initialising || node == null) {
            return;
        }
        node.name = text.isBlank() ? null : text;
        this.record(node, "$name", List.of());
    }

    /** Records the change since the last snapshot as an undo step (consecutive edits of one field merge). */
    private void record(MacroNode node, String field, List<Connection> removedWires) {
        final Workflow workflow = this.state.current();
        final EditNodeCommand.Snapshot after = EditNodeCommand.Snapshot.of(node);
        if (workflow != null && this.snapshot != null && !after.equals(this.snapshot)) {
            this.state.history().record(new EditNodeCommand(workflow, node.id, field, this.snapshot, after, removedWires));
        }
        this.snapshot = after;
        this.edited();
    }

    private void edited() {
        this.state.markDirty();
        this.issuesStale = true;
    }

    private void ensureBuilt(MacroNode node, Workflow workflow) {
        if (node.id.equals(this.builtFor) && workflow == this.builtWorkflow && this.builtEpoch == this.state.epoch()) {
            return;
        }
        if (!node.id.equals(this.builtFor) || workflow != this.builtWorkflow) {
            this.scroll = 0;
        }
        this.builtFor = node.id;
        this.builtWorkflow = workflow;
        this.builtEpoch = this.state.epoch();
        this.snapshot = EditNodeCommand.Snapshot.of(node);
        this.issuesStale = true;
        this.initialising = true;
        this.nameField.setText(node.name == null ? "" : node.name);
        this.nameField.setFocused(false);
        this.initialising = false;
        final NodeType type = NodeTypes.resolve(node.type);
        this.nameField.placeholder(type.displayName());
        final FieldContext ctx = new FieldContext(this.state.workflows(), workflow, node, key -> {
            final List<Connection> wiresBefore = new ArrayList<>(workflow.connections);
            GraphEdit.pruneConnections(workflow, node);
            final List<Connection> removed = new ArrayList<>();
            for (Connection wire : wiresBefore) {
                if (!workflow.connections.contains(wire)) {
                    removed.add(wire);
                }
            }
            this.record(node, key, removed);
        }, true);
        this.fields = new ParamFieldList(type.params(), node.params, ctx);
    }

    private void refreshIssues(MacroNode node, Workflow workflow) {
        this.issues = this.state.validation().forNode(workflow, node.id);
    }

    // ------------------------------------------------------------------ rendering

    public void render(GuiGraphicsExtractor g, int mx, int my) {
        final MacroNode node = this.node();
        final Workflow workflow = this.state.current();
        if (node == null || workflow == null) {
            return;
        }
        this.ensureBuilt(node, workflow);
        this.refreshIssues(node, workflow);
        final NodeType type = NodeTypes.resolve(node.type);

        RenderUtil.rect(g, this.x, this.y, this.w, this.h, Theme.POPOVER | 0xFF000000);
        RenderUtil.rect(g, this.x, this.y, 1, this.h, Theme.POPOVER_BORDER);

        // header
        RenderUtil.roundedRect(g, this.x + PAD, this.y + 7, 20, 20, 5, NodeRenderer.categoryColor(type.category()));
        UiText.drawCentered(g, type.icon(), this.x + PAD + 10, this.y + 12, 0xFFFFFFFF, UiText.Weight.SEMIBOLD);
        UiText.draw(g, UiText.ellipsize(type.displayName(), this.w - 2 * PAD - 60, UiText.Weight.SEMIBOLD), this.x + PAD + 28, this.y + 12, Theme.TEXT, UiText.Weight.SEMIBOLD);
        FieldUi.iconButton(g, this.x + this.w - PAD - FieldUi.ICON, this.y + (HEADER_H - FieldUi.ICON) / 2, FieldUi.Icon.CLOSE, true, mx, my);
        RenderUtil.rect(g, this.x + 1, this.y + HEADER_H - 1, this.w - 1, 1, Theme.DIVIDER);

        // content
        final int cx = this.x + PAD;
        final int cw = this.w - 2 * PAD - 4;
        final int top = this.y + HEADER_H;
        final int viewH = this.h - HEADER_H;
        final int max = Math.max(0, this.contentHeight - viewH + PAD);
        this.scroll = Math.max(0, Math.min(this.scroll, max));
        RenderUtil.scissor(g, this.x + 1, top, this.w - 1, viewH);
        int cy = top + PAD - Math.round(this.scroll);

        FieldUi.label(g, "Name", cx, cy);
        this.nameField.layout(cx, cy + 12, cw, 22);
        this.nameField.render(g, mx, my);
        cy += 12 + 22 + ParamFieldList.GAP + 4;

        if (type.category() != NodeCategory.NOTE && NodeTypes.isKnown(node.type)) {
            UiText.draw(g, "Disabled", cx, cy + 4, Theme.TEXT);
            FieldUi.toggle(g, cx + cw - FieldUi.TOGGLE_W, cy + 3, node.disabled);
            this.disabledRow = new int[]{cx, cy, cw, 18};
            UiText.draw(g, "A disabled node ends the path there", cx, cy + 18, Theme.TEXT_FAINT);
            cy += 18 + UiText.lineHeight() + 2 + ParamFieldList.GAP;
        } else {
            this.disabledRow = new int[4];
        }

        if (!NodeTypes.isKnown(node.type)) {
            for (FormattedCharSequence line : UiText.wrap("This node type is unknown to this version of SkySens. It is kept as it is and never runs.", cw)) {
                UiText.drawLine(g, line, cx, cy, Theme.TEXT_MUTED);
                cy += UiText.lineHeight() + 1;
            }
        } else if (this.fields != null) {
            this.fields.render(g, cx, cy, cw, mx, my);
            cy += this.fields.height(cw);
        }

        cy += 14;
        if (!this.issues.isEmpty()) {
            RenderUtil.rect(g, cx, cy, cw, 1, Theme.DIVIDER);
            cy += 8;
            UiText.draw(g, "Issues", cx, cy, Theme.TEXT_MUTED, UiText.Weight.SEMIBOLD);
            cy += 14;
            for (NodeIssue issue : this.issues) {
                final int color = switch (issue.severity()) {
                    case ERROR -> Theme.DANGER;
                    case WARNING -> FieldUi.AMBER;
                    case INFO -> Theme.TEXT_FAINT;
                };
                RenderUtil.circle(g, cx + 3, cy + 4, 3, color);
                for (FormattedCharSequence line : UiText.wrap(issue.message(), cw - 12)) {
                    UiText.drawLine(g, line, cx + 12, cy, Theme.TEXT_MUTED);
                    cy += UiText.lineHeight() + 1;
                }
                cy += 4;
            }
        }
        this.contentHeight = cy + Math.round(this.scroll) - top;
        RenderUtil.unscissor(g);
    }

    // ------------------------------------------------------------------ input

    /** A click inside the panel (the caller checked {@link #contains}). Always consumed. */
    public boolean mouseClicked(double mx, double my, int button) {
        this.blur();
        if (button == 0 && FieldUi.inIcon(mx, my, this.x + this.w - PAD - FieldUi.ICON, this.y + (HEADER_H - FieldUi.ICON) / 2)) {
            this.close();
            return true;
        }
        if (my < this.y + HEADER_H) {
            return true;
        }
        if (button == 0 && this.nameField.mouseClicked(mx, my)) {
            return true;
        }
        final MacroNode shown = this.node();
        if (button == 0 && shown != null && this.disabledRow[2] > 0
                && RenderUtil.inside(mx, my, this.disabledRow[0], this.disabledRow[1], this.disabledRow[2], this.disabledRow[3])) {
            final Workflow workflow = this.state.current();
            if (workflow != null) {
                this.state.history().execute(new ToggleDisabledCommand(workflow, List.of(shown)));
                OptionWidget.playClick();
            }
            return true;
        }
        if (this.fields != null) {
            this.fields.mouseClicked(mx, my, button);
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!this.contains(mx, my)) {
            return false;
        }
        this.scroll -= (float) (scrollY * 30);
        return true;
    }

    public boolean keyPressed(KeyEvent e) {
        if (this.nameField.isFocused()) {
            return this.nameField.keyPressed(e);
        }
        return this.fields != null && this.fields.keyPressed(e);
    }

    public boolean charTyped(CharacterEvent e) {
        if (this.nameField.isFocused()) {
            return this.nameField.charTyped(e);
        }
        return this.fields != null && this.fields.charTyped(e);
    }

    /** Params of the shown node, for tests/debug. */
    public @Nullable JsonObject params() {
        final MacroNode node = this.node();
        return node == null ? null : node.params;
    }

}
