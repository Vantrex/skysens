package de.vantrex.skysens.client.features.macro.editor.list;

import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.Consumer;

/**
 * The left sidebar: the workflows in evaluation order. Click selects a workflow, the knob switches it on or off.
 * (Creating, renaming, reordering and deleting arrive with the editing milestone.)
 */
public final class MacroListPanel {

    public static final int TITLE_HEIGHT = 34;
    private static final int ROW_HEIGHT = 24;
    private static final int TOGGLE_W = 22;
    private static final int TOGGLE_H = 12;
    private static final int PAD = 8;

    private final EditorState state;
    private final Consumer<Workflow> onToggle;

    private int x;
    private int y;
    private int w;
    private int h;
    private float scroll;
    private int footerHeight;

    public MacroListPanel(EditorState state, Consumer<Workflow> onToggle) {
        this.state = state;
        this.onToggle = onToggle;
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

    /** The whole panel rectangle {x, y, w, h}, the tutorial's anchor. */
    public int[] rect() {
        return new int[]{this.x, this.y, this.w, this.h};
    }

    private int listTop() {
        return this.y + TITLE_HEIGHT;
    }

    private int listHeight() {
        return this.h - TITLE_HEIGHT - this.footerHeight;
    }

    public void render(GuiGraphicsExtractor g, int mx, int my) {
        UiText.draw(g, "Macros", this.x + PAD + 2, this.y + (TITLE_HEIGHT - 9) / 2, Theme.TEXT, UiText.Weight.SEMIBOLD);
        final List<Workflow> workflows = this.state.workflows();
        UiText.drawRight(g, String.valueOf(workflows.size()), this.x + this.w - PAD - 2, this.y + (TITLE_HEIGHT - 9) / 2, Theme.TEXT_FAINT);
        RenderUtil.rect(g, this.x, this.y + TITLE_HEIGHT - 1, this.w, 1, Theme.DIVIDER);

        // footer hint, wrapped
        final var hint = UiText.wrap("Evaluated top to bottom. Higher macros win when they share a key.", this.w - 2 * PAD - 4);
        this.footerHeight = hint.size() * (UiText.lineHeight() + 2) + 14;
        RenderUtil.rect(g, this.x, this.y + this.h - this.footerHeight, this.w, 1, Theme.DIVIDER);
        int hy = this.y + this.h - this.footerHeight + 8;
        for (var line : hint) {
            UiText.drawLine(g, line, this.x + PAD + 2, hy, Theme.TEXT_FAINT);
            hy += UiText.lineHeight() + 2;
        }

        final int top = this.listTop();
        final int height = this.listHeight();
        final int content = workflows.size() * ROW_HEIGHT + 8;
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, content - height)));
        if (workflows.isEmpty()) {
            UiText.draw(g, "No macros yet", this.x + PAD + 2, top + 10, Theme.TEXT_FAINT);
            return;
        }
        RenderUtil.scissor(g, this.x, top, this.w, height);
        for (int i = 0; i < workflows.size(); i++) {
            final Workflow workflow = workflows.get(i);
            final int ry = this.rowY(i);
            if (ry + ROW_HEIGHT < top || ry > top + height) {
                continue;
            }
            final boolean selected = workflow == this.state.current();
            final boolean hovered = RenderUtil.inside(mx, my, this.x + 4, ry, this.w - 8, ROW_HEIGHT) && my >= top && my < top + height;
            if (selected) {
                RenderUtil.roundedRect(g, this.x + 4, ry, this.w - 8, ROW_HEIGHT - 2, Theme.CARD_RADIUS, Theme.ACCENT_SOFT);
            } else if (hovered) {
                RenderUtil.roundedRect(g, this.x + 4, ry, this.w - 8, ROW_HEIGHT - 2, Theme.CARD_RADIUS, Theme.HOVER_OVERLAY);
            }
            this.renderToggle(g, this.toggleX(), ry + (ROW_HEIGHT - 2 - TOGGLE_H) / 2, workflow.active);
            final int textX = this.toggleX() + TOGGLE_W + 8;
            final String name = UiText.ellipsize(workflow.name, this.x + this.w - PAD - textX - 2);
            UiText.draw(g, name, textX, ry + (ROW_HEIGHT - 2 - 9) / 2, workflow.active ? Theme.TEXT : Theme.TEXT_MUTED,
                    selected ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR);
        }
        RenderUtil.unscissor(g);
    }

    private int toggleX() {
        return this.x + PAD + 2;
    }

    private int rowY(int index) {
        return this.listTop() + 4 + index * ROW_HEIGHT - Math.round(this.scroll);
    }

    private void renderToggle(GuiGraphicsExtractor g, int tx, int ty, boolean on) {
        RenderUtil.roundedRect(g, tx, ty, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, on ? Theme.ACCENT : Theme.TRACK);
        final int r = (TOGGLE_H - 4) / 2;
        RenderUtil.knob(g, on ? tx + TOGGLE_W - 2 - r : tx + 2 + r, ty + TOGGLE_H / 2, r, on ? 1f : 0f);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!this.contains(mx, my) || button != 0 || my < this.listTop() || my >= this.listTop() + this.listHeight()) {
            return this.contains(mx, my);
        }
        final List<Workflow> workflows = this.state.workflows();
        for (int i = 0; i < workflows.size(); i++) {
            final int ry = this.rowY(i);
            if (my >= ry && my < ry + ROW_HEIGHT - 2) {
                final Workflow workflow = workflows.get(i);
                if (mx >= this.toggleX() - 2 && mx < this.toggleX() + TOGGLE_W + 2) {
                    this.onToggle.accept(workflow);
                } else {
                    this.state.select(workflow);
                }
                return true;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!this.contains(mx, my)) {
            return false;
        }
        final int content = this.state.workflows().size() * ROW_HEIGHT + 8;
        this.scroll = (float) Math.max(0, Math.min(Math.max(0, content - this.listHeight()), this.scroll - scrollY * 24));
        return true;
    }

}
