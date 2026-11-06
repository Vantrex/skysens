package de.vantrex.skysens.client.features.macro.editor.executions;

import de.vantrex.skysens.client.features.macro.condition.RuleResult;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.editor.canvas.NodeRenderer;
import de.vantrex.skysens.client.features.macro.engine.ExecutionLog;
import de.vantrex.skysens.client.features.macro.engine.ExecutionRecord;
import de.vantrex.skysens.client.features.macro.engine.GraphWalker;
import de.vantrex.skysens.client.features.macro.engine.Resolution;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.logic.SwitchNode;
import de.vantrex.skysens.client.features.macro.condition.RuleGroup;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The Executions tab: recent macro key presses, newest first. A row shows the chord, how it was pressed, the outcome
 * chip and the chosen workflow and action; expanding it lists every path with the per-rule results (tick, cross or a
 * grey "?" for unknown, with the reason). Clicking a row pins its highlight on the canvas; clicking a path also
 * switches to that path's workflow.
 */
public final class ExecutionsPanel {

    private static final int HEADER_HEIGHT = 26;
    private static final int PAD = 8;
    private static final int ROW_HEIGHT = 36;
    private static final int ROW_GAP = 4;
    private static final int LINE = 12;
    private static final int RED = 0xFFE5534B;
    private static final int GREY = 0xFF9AA0A6;
    private static final int MAX_RECORDS = 200;

    private record Hit(int y, int h, ExecutionRecord record, @Nullable Workflow workflow) {
    }

    private final EditorState state;
    private final Consumer<Workflow> selectWorkflow;
    private final Set<ExecutionRecord> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Hit> hits = new ArrayList<>();

    private int x;
    private int y;
    private int w;
    private int h;
    private float scroll;
    private int contentHeight;

    public ExecutionsPanel(EditorState state, Consumer<Workflow> selectWorkflow) {
        this.state = state;
        this.selectWorkflow = selectWorkflow;
    }

    public void layout(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(double mx, double my) {
        return RenderUtil.inside(mx, my, this.x, this.y, this.w, this.h);
    }

    // ------------------------------------------------------------------ rendering

    public void render(GuiGraphicsExtractor g, int mx, int my) {
        final List<ExecutionRecord> records = ExecutionLog.getInstance().recent(MAX_RECORDS);
        UiText.draw(g, "Executions", this.x + PAD, this.y + (HEADER_HEIGHT - 9) / 2, Theme.TEXT, UiText.Weight.SEMIBOLD);
        UiText.drawRight(g, records.size() + (records.size() == 1 ? " press" : " presses"), this.x + this.w - PAD,
                this.y + (HEADER_HEIGHT - 9) / 2, Theme.TEXT_FAINT);
        RenderUtil.rect(g, this.x, this.y + HEADER_HEIGHT - 1, this.w, 1, Theme.DIVIDER);

        final int listY = this.y + HEADER_HEIGHT;
        final int listH = this.h - HEADER_HEIGHT;
        this.hits.clear();
        if (records.isEmpty()) {
            final int cx = this.x + this.w / 2;
            UiText.drawCentered(g, "No key presses yet", cx, listY + 24, Theme.TEXT_MUTED, UiText.Weight.SEMIBOLD);
            int ty = listY + 40;
            for (var line : UiText.wrap("Press a key that has a Hotkey trigger in game. Every press shows up here, with the path it took and why.", this.w - 2 * PAD - 8)) {
                UiText.drawLine(g, line, cx - this.w / 2 + PAD + 4, ty, Theme.TEXT_FAINT);
                ty += LINE;
            }
            this.contentHeight = 0;
            this.scroll = 0;
            return;
        }

        RenderUtil.scissor(g, this.x, listY, this.w, listH);
        int cursor = listY + 6 - Math.round(this.scroll);
        final long now = System.currentTimeMillis();
        for (ExecutionRecord record : records) {
            final int height = this.row(g, record, cursor, listY, listH, mx, my, now);
            cursor += height + ROW_GAP;
        }
        RenderUtil.unscissor(g);
        this.contentHeight = cursor - (listY - Math.round(this.scroll)) + 2;
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, this.contentHeight - listH)));
        this.renderScrollbar(g, listY, listH);
    }

    private void renderScrollbar(GuiGraphicsExtractor g, int listY, int listH) {
        if (this.contentHeight <= listH) {
            return;
        }
        final int barH = Math.max(16, listH * listH / this.contentHeight);
        final int barY = listY + (int) ((listH - barH) * (this.scroll / (this.contentHeight - listH)));
        RenderUtil.roundedRect(g, this.x + this.w - 4, barY, 3, barH, 1, RenderUtil.withAlpha(Theme.TEXT_FAINT, 0.6f));
    }

    /** Draws (when visible) one record and returns its height. */
    private int row(GuiGraphicsExtractor g, ExecutionRecord record, int top, int clipTop, int clipH, int mx, int my, long now) {
        final boolean open = this.expanded.contains(record);
        final int bodyHeight = open ? this.body(g, record, top + ROW_HEIGHT, false, 0, 0) : 0;
        final int height = ROW_HEIGHT + bodyHeight;
        final int rowX = this.x + 6;
        final int rowW = this.w - 12 - (this.contentHeight > this.h ? 4 : 0);
        this.hits.add(new Hit(top, ROW_HEIGHT, record, null));
        if (top + height < clipTop || top > clipTop + clipH) {
            if (open) {
                this.addPathHits(record, top + ROW_HEIGHT);
            }
            return height;
        }

        final boolean pinned = this.state.pinned() == record;
        final boolean hovered = RenderUtil.inside(mx, my, rowX, top, rowW, ROW_HEIGHT) && my >= clipTop && my < clipTop + clipH;
        RenderUtil.roundedRectWithBorder(g, rowX, top, rowW, height, 5, hovered ? Theme.CARD_HOVER : Theme.CARD,
                pinned ? Theme.ACCENT : Theme.BORDER);

        final Resolution resolution = record.resolution();
        final int chipRight = rowX + rowW - 8;
        final int chipW = this.chip(g, chipRight, top + 6, outcomeLabel(resolution.outcome()), outcomeColor(resolution.outcome()));
        final String chord = UiText.ellipsize(record.chord().displayName(), rowW - 16 - chipW - 8, UiText.Weight.SEMIBOLD);
        UiText.draw(g, chord, rowX + 8, top + 6, Theme.TEXT, UiText.Weight.SEMIBOLD);
        final int modeX = rowX + 8 + UiText.width(chord, UiText.Weight.SEMIBOLD) + 6;
        final String mode = UiText.enumLabel(record.mode()).toLowerCase() + (record.inGui() ? " (inventory)" : "");
        UiText.draw(g, UiText.ellipsize(mode, Math.max(0, chipRight - chipW - 6 - modeX)), modeX, top + 6, Theme.TEXT_FAINT);

        final GraphWalker.PathResult chosen = resolution.chosenPath();
        String summary = chosen != null ? chosen.describe() : resolution.message() != null ? resolution.message() : "No macro matched";
        if (chosen != null && resolution.message() != null && !resolution.ran()) {
            summary = resolution.message();
        }
        final String ago = ago(now - record.time());
        UiText.draw(g, ago, rowX + 8, top + 20, Theme.TEXT_FAINT);
        UiText.draw(g, UiText.ellipsize(summary, rowW - 24 - UiText.width(ago)), rowX + 8 + UiText.width(ago) + 8, top + 20, Theme.TEXT_MUTED);

        if (open) {
            RenderUtil.rect(g, rowX + 6, top + ROW_HEIGHT - 1, rowW - 12, 1, Theme.DIVIDER);
            this.body(g, record, top + ROW_HEIGHT, true, mx, my);
            this.addPathHits(record, top + ROW_HEIGHT);
        }
        return height;
    }

    private void addPathHits(ExecutionRecord record, int top) {
        int cursor = top + 4;
        for (GraphWalker.PathResult path : record.resolution().paths()) {
            final int height = this.pathHeight(path);
            this.hits.add(new Hit(cursor, height, record, path.ref().workflow()));
            cursor += height + 4;
        }
    }

    /** Draws the expanded part of a record (or only measures it) and returns its height. */
    private int body(GuiGraphicsExtractor g, ExecutionRecord record, int top, boolean draw, int mx, int my) {
        int cursor = top + 4;
        final int left = this.x + 6 + 10;
        final int right = this.x + this.w - 6 - 10 - (this.contentHeight > this.h ? 4 : 0);
        final Resolution resolution = record.resolution();
        final GraphWalker.PathResult chosen = resolution.chosenPath();
        if (resolution.paths().isEmpty() && draw) {
            UiText.draw(g, "No path", left, cursor + 1, Theme.TEXT_FAINT);
        }
        for (GraphWalker.PathResult path : resolution.paths()) {
            final int height = this.pathHeight(path);
            if (draw) {
                this.path(g, path, path == chosen, cursor, left, right);
            }
            cursor += height + 4;
        }
        if (resolution.paths().isEmpty()) {
            cursor += LINE;
        }
        if (resolution.message() != null) {
            if (draw) {
                UiText.draw(g, UiText.ellipsize(resolution.message(), right - left), left, cursor, Theme.TEXT_FAINT);
            }
            cursor += LINE;
        }
        return cursor - top + 2;
    }

    private int pathHeight(GraphWalker.PathResult path) {
        int lines = 1;
        for (GraphWalker.LogicStep step : path.steps()) {
            lines += 1 + step.results().size();
        }
        if (path.action() != null) {
            lines++;
        }
        return lines * LINE;
    }

    private void path(GuiGraphicsExtractor g, GraphWalker.PathResult path, boolean chosen, int top, int left, int right) {
        final Workflow workflow = path.ref().workflow();
        int cursor = top;
        RenderUtil.circle(g, left + 3, cursor + 5, 3, chosen ? NodeRenderer.GREEN : GREY);
        final String end = endLabel(path.end());
        final int endW = UiText.width(end);
        UiText.draw(g, UiText.ellipsize(workflow.name, right - left - 12 - endW - 6, UiText.Weight.SEMIBOLD), left + 10, cursor, Theme.TEXT, UiText.Weight.SEMIBOLD);
        UiText.drawRight(g, end, right, cursor, Theme.TEXT_FAINT);
        cursor += LINE;
        for (GraphWalker.LogicStep step : path.steps()) {
            final MacroNode node = findNode(workflow, step.nodeId());
            final String port = step.port() == null ? "end" : portLabel(node, step.port());
            UiText.draw(g, UiText.ellipsize(step.nodeName() + " → " + port, right - left - 10), left + 10, cursor, Theme.TEXT_MUTED);
            cursor += LINE;
            int index = 0;
            for (RuleResult result : step.results()) {
                index++;
                this.ruleLine(g, result, ruleLabel(node, result, index), cursor, left + 14, right);
                cursor += LINE;
            }
        }
        if (path.action() != null && path.actionType() != null) {
            final String action = path.actionType().displayName() + " \"" + path.actionType().summary(path.action()) + "\"";
            UiText.draw(g, UiText.ellipsize("→ " + action, right - left - 10), left + 10, cursor, Theme.TEXT_MUTED);
        }
    }

    private void ruleLine(GuiGraphicsExtractor g, RuleResult result, String label, int top, int left, int right) {
        final int cy = top + 5;
        switch (result.value()) {
            case TRUE -> check(g, left, cy, NodeRenderer.GREEN);
            case FALSE -> cross(g, left, cy, RED);
            case UNKNOWN -> UiText.drawCentered(g, "?", left + 3, top, GREY, UiText.Weight.SEMIBOLD);
        }
        final int textX = left + 12;
        final String shown = UiText.ellipsize(label, right - textX);
        final int color = result.value() == Tri.UNKNOWN ? GREY : Theme.TEXT;
        UiText.draw(g, shown, textX, top, color);
        if (result.unknownReason() != null && !result.unknownReason().isEmpty()) {
            final int reasonX = textX + UiText.width(shown) + 6;
            if (reasonX < right - 20) {
                UiText.draw(g, UiText.ellipsize("(" + result.unknownReason() + ")", right - reasonX), reasonX, top, Theme.TEXT_FAINT);
            }
        }
    }

    /** Small tick drawn from pixels, so it does not depend on the font having the glyph. 7 wide, 5 high, centred on cy. */
    private static void check(GuiGraphicsExtractor g, int x, int cy, int color) {
        final int[][] px = {{0, 2}, {1, 3}, {2, 4}, {3, 3}, {4, 2}, {5, 1}, {6, 0}};
        for (int[] p : px) {
            g.fill(x + p[0], cy - 2 + p[1], x + p[0] + 1, cy - 2 + p[1] + 2, color);
        }
    }

    /** Small cross, 6x6. */
    private static void cross(GuiGraphicsExtractor g, int x, int cy, int color) {
        for (int i = 0; i < 6; i++) {
            g.fill(x + i, cy - 3 + i, x + i + 1, cy - 3 + i + 1, color);
            g.fill(x + 5 - i, cy - 3 + i, x + 6 - i, cy - 3 + i + 1, color);
        }
    }

    /** Draws a rounded chip right-aligned at {@code right}; returns its width. */
    private int chip(GuiGraphicsExtractor g, int right, int top, String label, int color) {
        final int width = UiText.width(label) + 10;
        RenderUtil.roundedRect(g, right - width, top, width, 13, 6, RenderUtil.withAlpha(color, 0.22f));
        UiText.draw(g, label, right - width + 5, top + 2, color);
        return width;
    }

    // ------------------------------------------------------------------ labels

    private static String ago(long ms) {
        final long seconds = Math.max(0, ms / 1000);
        if (seconds < 2) {
            return "just now";
        }
        if (seconds < 60) {
            return seconds + "s ago";
        }
        if (seconds < 3600) {
            return seconds / 60 + "m ago";
        }
        return seconds / 3600 + "h ago";
    }

    private static String outcomeLabel(Resolution.Outcome outcome) {
        return switch (outcome) {
            case RAN -> "Ran";
            case RATE_LIMITED -> "Rate-limited";
            case ON_COOLDOWN -> "Cooldown";
            case FAILED -> "Failed";
            case NO_ACTION -> "No action";
            case NO_MATCH_UNKNOWN_STATE -> "Unknown state";
        };
    }

    private static int outcomeColor(Resolution.Outcome outcome) {
        return switch (outcome) {
            case RAN -> NodeRenderer.GREEN;
            case RATE_LIMITED, ON_COOLDOWN -> NodeRenderer.AMBER;
            case FAILED -> RED;
            case NO_ACTION, NO_MATCH_UNKNOWN_STATE -> GREY;
        };
    }

    private static String endLabel(GraphWalker.End end) {
        return switch (end) {
            case ACTION -> "action";
            case NO_ACTION -> "no action";
            case DISABLED -> "path ends at disabled node";
            case INVALID -> "broken wire";
            case END_UNKNOWN -> "state unknown";
        };
    }

    private static @Nullable MacroNode findNode(Workflow workflow, String id) {
        for (MacroNode node : workflow.nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    private static String portLabel(@Nullable MacroNode node, String portId) {
        if (node != null) {
            final NodeType type = NodeTypes.resolve(node.type);
            for (Port port : type.outputs(node)) {
                if (port.id().equals(portId)) {
                    return port.label().isEmpty() ? portId : port.label();
                }
            }
        }
        return portId;
    }

    /** "Conditions" for an If, the rule's name (or "Rule n") for a Switch rule. */
    private static String ruleLabel(@Nullable MacroNode node, RuleResult result, int index) {
        if (node != null && SwitchNode.ID.equals(node.type)) {
            for (RuleGroup rule : SwitchNode.rules(node)) {
                if (rule.id().equals(result.ruleId())) {
                    return rule.name().isBlank() ? "Rule " + index : rule.name();
                }
            }
            return "Rule " + index;
        }
        return "Conditions";
    }

    // ------------------------------------------------------------------ input

    public boolean mouseClicked(double mx, double my, int button) {
        if (!this.contains(mx, my) || my < this.y + HEADER_HEIGHT) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        // path hits were added after their row's hit, so the last match is the most specific one
        Hit found = null;
        for (Hit hit : this.hits) {
            if (my >= hit.y && my < hit.y + hit.h) {
                found = hit;
            }
        }
        if (found == null) {
            return true;
        }
        final ExecutionRecord record = found.record;
        if (found.workflow != null) {
            this.state.setPinned(record);
            this.selectWorkflow.accept(found.workflow);
            return true;
        }
        if (this.expanded.remove(record)) {
            if (this.state.pinned() == record) {
                this.state.setPinned(null);
            }
        } else {
            this.expanded.add(record);
            this.state.setPinned(record);
            final GraphWalker.PathResult chosen = record.resolution().chosenPath();
            final GraphWalker.PathResult shown = chosen != null ? chosen
                    : record.resolution().paths().isEmpty() ? null : record.resolution().paths().get(0);
            if (shown != null) {
                this.selectWorkflow.accept(shown.ref().workflow());
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!this.contains(mx, my)) {
            return false;
        }
        final int listH = this.h - HEADER_HEIGHT;
        this.scroll = (float) Math.max(0, Math.min(Math.max(0, this.contentHeight - listH), this.scroll - scrollY * 30));
        return true;
    }

}
