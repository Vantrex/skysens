package de.vantrex.skysens.client.features.macro.editor.list;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.ui.SearchField;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiScale;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The left sidebar: the workflows in evaluation order. Click selects, the knob switches a macro on or off, "+" creates
 * a macro (with one Hotkey trigger), double-click or the menu renames it inline, dragging a row reorders (the order is
 * the evaluation order), and the right-click menu duplicates or deletes (with a confirmation).
 */
public final class MacroListPanel {

    public static final int TITLE_HEIGHT = 34;
    private static final int ROW_HEIGHT = 24;
    private static final int TOGGLE_W = 22;
    private static final int TOGGLE_H = 12;
    private static final int PAD = 8;
    private static final int ADD_SIZE = 20;
    private static final int DRAG_THRESHOLD = 4;
    private static final int MENU_W = 130;
    private static final int MENU_ITEM_H = 20;
    private static final String[] MENU = {"Rename", "Duplicate", "Delete"};

    private final EditorState state;
    private final Consumer<Workflow> onToggle;
    private final Runnable onChanged;
    private final SearchField renameField = new SearchField(text -> { }).noHint().maxLength(40).placeholder("Macro name");

    private int x;
    private int y;
    private int w;
    private int h;
    private float scroll;
    private int footerHeight;

    private @Nullable Workflow renaming;

    // drag to reorder
    private int pressIndex = -1;
    private double pressY;
    private boolean dragging;
    private int dropSlot;
    private double dragY;

    // context menu
    private @Nullable Workflow menuFor;
    private int menuX;
    private int menuY;
    private boolean confirmDelete;

    public MacroListPanel(EditorState state, Consumer<Workflow> onToggle, Runnable onChanged) {
        this.state = state;
        this.onToggle = onToggle;
        this.onChanged = onChanged;
        this.renameField.onEnter(this::commitRename);
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

    public boolean isEditing() {
        return this.renaming != null;
    }

    public boolean menuOpen() {
        return this.menuFor != null;
    }

    public void closeMenu() {
        this.menuFor = null;
        this.confirmDelete = false;
    }

    private int listTop() {
        return this.y + TITLE_HEIGHT;
    }

    private int listHeight() {
        return this.h - TITLE_HEIGHT - this.footerHeight;
    }

    private int[] addRect() {
        return new int[]{this.x + this.w - PAD - ADD_SIZE, this.y + (TITLE_HEIGHT - ADD_SIZE) / 2, ADD_SIZE, ADD_SIZE};
    }

    // ------------------------------------------------------------------ rendering

    public void render(GuiGraphicsExtractor g, int mx, int my) {
        UiText.draw(g, "Macros", this.x + PAD + 2, this.y + (TITLE_HEIGHT - 9) / 2, Theme.TEXT, UiText.Weight.SEMIBOLD);
        final List<Workflow> workflows = this.state.workflows();
        final int[] add = this.addRect();
        UiText.drawRight(g, String.valueOf(workflows.size()), add[0] - 6, this.y + (TITLE_HEIGHT - 9) / 2, Theme.TEXT_FAINT);
        final boolean addHover = RenderUtil.inside(mx, my, add[0], add[1], add[2], add[3]);
        RenderUtil.roundedRect(g, add[0], add[1], add[2], add[3], 5, addHover ? Theme.ACCENT_HOVER : Theme.ACCENT);
        RenderUtil.rect(g, add[0] + add[2] / 2 - 4, add[1] + add[3] / 2, 9, 1, 0xFFFFFFFF);
        RenderUtil.rect(g, add[0] + add[2] / 2, add[1] + add[3] / 2 - 4, 1, 9, 0xFFFFFFFF);
        RenderUtil.rect(g, this.x, this.y + TITLE_HEIGHT - 1, this.w, 1, Theme.DIVIDER);

        // footer hint, wrapped
        final var hint = UiText.wrap("Evaluated top to bottom. Higher macros win when they share a key. Drag to reorder.", this.w - 2 * PAD - 4);
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
            UiText.draw(g, "No macros yet. Press +", this.x + PAD + 2, top + 10, Theme.TEXT_FAINT);
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
            final boolean hovered = !this.dragging && this.menuFor == null && RenderUtil.inside(mx, my, this.x + 4, ry, this.w - 8, ROW_HEIGHT) && my >= top && my < top + height;
            if (this.dragging && i == this.pressIndex) {
                RenderUtil.roundedRect(g, this.x + 4, ry, this.w - 8, ROW_HEIGHT - 2, Theme.CARD_RADIUS, Theme.HOVER_OVERLAY);
            } else if (selected) {
                RenderUtil.roundedRect(g, this.x + 4, ry, this.w - 8, ROW_HEIGHT - 2, Theme.CARD_RADIUS, Theme.ACCENT_SOFT);
            } else if (hovered) {
                RenderUtil.roundedRect(g, this.x + 4, ry, this.w - 8, ROW_HEIGHT - 2, Theme.CARD_RADIUS, Theme.HOVER_OVERLAY);
            }
            this.renderRow(g, workflow, ry, selected, mx, my);
        }
        if (this.dragging) {
            final int lineY = this.listTop() + 4 + this.dropSlot * ROW_HEIGHT - Math.round(this.scroll) - 1;
            RenderUtil.rect(g, this.x + 6, lineY, this.w - 12, 2, Theme.ACCENT);
            final Workflow dragged = workflows.get(this.pressIndex);
            final int gy = (int) this.dragY - ROW_HEIGHT / 2;
            RenderUtil.roundedRect(g, this.x + 4, gy, this.w - 8, ROW_HEIGHT - 2, Theme.CARD_RADIUS, RenderUtil.withAlpha(Theme.ACCENT, 0.5f));
            UiText.draw(g, UiText.ellipsize(dragged.name, this.w - 40), this.x + PAD + 6, gy + (ROW_HEIGHT - 2 - 9) / 2, Theme.TEXT);
        }
        RenderUtil.unscissor(g);
    }

    private void renderRow(GuiGraphicsExtractor g, Workflow workflow, int ry, boolean selected, int mx, int my) {
        this.renderToggle(g, this.toggleX(), ry + (ROW_HEIGHT - 2 - TOGGLE_H) / 2, workflow.active);
        final int textX = this.toggleX() + TOGGLE_W + 8;
        if (workflow == this.renaming) {
            this.renameField.layout(textX - 4, ry + 1, this.x + this.w - PAD - textX + 4, ROW_HEIGHT - 4);
            this.renameField.render(g, mx, my);
            return;
        }
        final String name = UiText.ellipsize(workflow.name, this.x + this.w - PAD - textX - 2);
        UiText.draw(g, name, textX, ry + (ROW_HEIGHT - 2 - 9) / 2, workflow.active ? Theme.TEXT : Theme.TEXT_MUTED,
                selected ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR);
    }

    /** The context menu; drawn last so it sits above the canvas and sidebar. */
    public void renderOverlay(GuiGraphicsExtractor g, int mx, int my) {
        if (this.menuFor == null) {
            return;
        }
        final int items = this.confirmDelete ? 2 : MENU.length;
        final int height = items * MENU_ITEM_H + 6 + (this.confirmDelete ? MENU_ITEM_H : 0);
        final int sw = UiScale.width();
        final int sh = UiScale.height();
        this.menuX = Math.min(this.menuX, sw - MENU_W - 4);
        this.menuY = Math.min(this.menuY, sh - height - 4);
        RenderUtil.shadow(g, this.menuX, this.menuY, MENU_W, height, 4);
        RenderUtil.roundedRectWithBorder(g, this.menuX, this.menuY, MENU_W, height, 4, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        if (this.confirmDelete) {
            UiText.draw(g, UiText.ellipsize("Delete \"" + this.menuFor.name + "\"?", MENU_W - 16), this.menuX + 8, this.menuY + 8, Theme.TEXT);
            this.menuItem(g, 1, "Delete", mx, my, Theme.DANGER, MENU_ITEM_H);
            this.menuItem(g, 2, "Cancel", mx, my, Theme.TEXT, MENU_ITEM_H);
            return;
        }
        for (int i = 0; i < MENU.length; i++) {
            this.menuItem(g, i, MENU[i], mx, my, i == 2 ? Theme.DANGER : Theme.TEXT, 0);
        }
    }

    private void menuItem(GuiGraphicsExtractor g, int index, String label, int mx, int my, int color, int offset) {
        final int iy = this.menuY + 3 + index * MENU_ITEM_H + (offset > 0 ? 0 : 0);
        if (RenderUtil.inside(mx, my, this.menuX + 3, iy, MENU_W - 6, MENU_ITEM_H)) {
            RenderUtil.roundedRect(g, this.menuX + 3, iy, MENU_W - 6, MENU_ITEM_H, 3, Theme.HOVER_OVERLAY);
        }
        UiText.draw(g, label, this.menuX + 10, iy + (MENU_ITEM_H - 9) / 2 + 1, color);
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

    // ------------------------------------------------------------------ operations

    private int indexAt(double my) {
        final List<Workflow> workflows = this.state.workflows();
        for (int i = 0; i < workflows.size(); i++) {
            final int ry = this.rowY(i);
            if (my >= ry && my < ry + ROW_HEIGHT - 2) {
                return i;
            }
        }
        return -1;
    }

    private void create() {
        final Workflow workflow = GraphEdit.newWorkflow(this.state.workflows());
        this.state.workflows().add(workflow);
        this.state.select(workflow);
        this.onChanged.run();
        OptionWidget.playClick();
    }

    private void startRename(Workflow workflow) {
        this.renaming = workflow;
        this.renameField.setText(workflow.name);
        this.renameField.setFocused(true);
    }

    private void commitRename() {
        if (this.renaming != null) {
            final String text = this.renameField.text().trim();
            if (!text.isEmpty() && !text.equals(this.renaming.name)) {
                this.renaming.name = text;
                this.onChanged.run();
            }
        }
        this.cancelRename();
    }

    private void cancelRename() {
        this.renaming = null;
        this.renameField.setFocused(false);
    }

    private void duplicate(Workflow workflow) {
        final Workflow copy = GraphEdit.duplicate(workflow, this.state.workflows());
        final List<Workflow> list = this.state.workflows();
        list.add(list.indexOf(workflow) + 1, copy);
        this.state.select(copy);
        this.onChanged.run();
    }

    private void delete(Workflow workflow) {
        final List<Workflow> list = this.state.workflows();
        final int index = list.indexOf(workflow);
        list.remove(workflow);
        if (workflow == this.state.current()) {
            this.state.select(list.isEmpty() ? null : list.get(Math.min(index, list.size() - 1)));
        }
        this.onChanged.run();
    }

    // ------------------------------------------------------------------ input

    /** Clicks while the menu is open (always consumed). */
    public boolean menuClicked(double mx, double my) {
        final Workflow workflow = this.menuFor;
        if (workflow == null) {
            return false;
        }
        final int items = this.confirmDelete ? 2 : MENU.length;
        final int height = items * MENU_ITEM_H + 6 + (this.confirmDelete ? MENU_ITEM_H : 0);
        if (!RenderUtil.inside(mx, my, this.menuX, this.menuY, MENU_W, height)) {
            this.closeMenu();
            return true;
        }
        final int index = (int) ((my - this.menuY - 3) / MENU_ITEM_H);
        if (this.confirmDelete) {
            if (index == 1) {
                this.delete(workflow);
                this.closeMenu();
            } else if (index == 2) {
                this.closeMenu();
            }
            return true;
        }
        switch (index) {
            case 0 -> {
                this.closeMenu();
                this.startRename(workflow);
            }
            case 1 -> {
                this.closeMenu();
                this.duplicate(workflow);
            }
            case 2 -> this.confirmDelete = true;
            default -> {
            }
        }
        return true;
    }

    public boolean mouseClicked(double mx, double my, int button, boolean doubleClick) {
        if (this.renaming != null) {
            final boolean onField = this.renameField.isOver(mx, my);
            if (onField) {
                this.renameField.mouseClicked(mx, my);
                return true;
            }
            this.commitRename();
        }
        if (!this.contains(mx, my)) {
            return false;
        }
        final int[] add = this.addRect();
        if (button == 0 && RenderUtil.inside(mx, my, add[0], add[1], add[2], add[3])) {
            this.create();
            return true;
        }
        if (my < this.listTop() || my >= this.listTop() + this.listHeight()) {
            return true;
        }
        final int index = this.indexAt(my);
        if (index < 0) {
            return true;
        }
        final Workflow workflow = this.state.workflows().get(index);
        if (button == 1) {
            this.state.select(workflow);
            this.menuFor = workflow;
            this.menuX = (int) mx;
            this.menuY = (int) my;
            this.confirmDelete = false;
            return true;
        }
        if (button != 0) {
            return true;
        }
        if (mx >= this.toggleX() - 2 && mx < this.toggleX() + TOGGLE_W + 2) {
            this.onToggle.accept(workflow);
            return true;
        }
        this.state.select(workflow);
        if (doubleClick) {
            this.startRename(workflow);
            return true;
        }
        this.pressIndex = index;
        this.pressY = my;
        this.dragging = false;
        return true;
    }

    public boolean mouseDragged(double mx, double my) {
        if (this.pressIndex < 0) {
            return false;
        }
        if (!this.dragging && Math.abs(my - this.pressY) >= DRAG_THRESHOLD) {
            this.dragging = true;
        }
        if (this.dragging) {
            this.dragY = my;
            final int n = this.state.workflows().size();
            final int slot = (int) Math.floor((my - this.listTop() - 4 + Math.round(this.scroll) + ROW_HEIGHT / 2.0) / ROW_HEIGHT);
            this.dropSlot = Math.max(0, Math.min(n, slot));
        }
        return true;
    }

    public boolean mouseReleased() {
        final boolean wasDragging = this.dragging;
        if (this.dragging && this.pressIndex >= 0) {
            final List<Workflow> list = this.state.workflows();
            final int from = this.pressIndex;
            final int to = this.dropSlot > from ? this.dropSlot - 1 : this.dropSlot;
            if (to != from && from < list.size()) {
                final Workflow moved = list.remove(from);
                list.add(Math.max(0, Math.min(list.size(), to)), moved);
                this.onChanged.run();
            }
        }
        this.pressIndex = -1;
        this.dragging = false;
        return wasDragging;
    }

    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!this.contains(mx, my)) {
            return false;
        }
        final int content = this.state.workflows().size() * ROW_HEIGHT + 8;
        this.scroll = (float) Math.max(0, Math.min(Math.max(0, content - this.listHeight()), this.scroll - scrollY * 24));
        return true;
    }

    public boolean keyPressed(KeyEvent e) {
        if (this.renaming == null) {
            return false;
        }
        if (e.key() == InputConstants.KEY_ESCAPE) {
            this.cancelRename();
            return true;
        }
        return this.renameField.keyPressed(e);
    }

    public boolean charTyped(CharacterEvent e) {
        return this.renaming != null && this.renameField.charTyped(e);
    }

}
