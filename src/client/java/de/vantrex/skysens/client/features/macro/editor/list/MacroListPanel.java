package de.vantrex.skysens.client.features.macro.editor.list;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.editor.EditorState;
import de.vantrex.skysens.client.features.macro.editor.clipboard.MacroClipboard;
import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.editor.field.FieldUi;
import de.vantrex.skysens.client.features.macro.editor.history.CompositeCommand;
import de.vantrex.skysens.client.features.macro.editor.history.EditorCommand;
import de.vantrex.skysens.client.features.macro.editor.history.WorkflowCommands;
import de.vantrex.skysens.client.features.macro.model.MacroDocument;
import de.vantrex.skysens.client.features.macro.template.MacroTemplates;
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

import java.util.ArrayList;
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
    private static final String[] MENU = {"Rename", "Duplicate", "Export", "Delete"};
    private static final int BUTTON_H = 20;
    private static final int TEMPLATE_ROW_H = 34;
    private static final int TEMPLATE_W = 230;

    private final EditorState state;
    private final Consumer<Workflow> onToggle;
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
    private boolean templatesOpen;
    private int[] importButton = new int[4];
    private int[] exportButton = new int[4];
    private int[] templatesButton = new int[4];

    public MacroListPanel(EditorState state, Consumer<Workflow> onToggle) {
        this.state = state;
        this.onToggle = onToggle;
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
        return this.menuFor != null || this.templatesOpen;
    }

    public void closeMenu() {
        this.menuFor = null;
        this.confirmDelete = false;
        this.templatesOpen = false;
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
        final int hintHeight = hint.size() * (UiText.lineHeight() + 2) + 14;
        final int buttonsHeight = 2 * BUTTON_H + 3 * 4 + 2;
        this.footerHeight = hintHeight + buttonsHeight;
        RenderUtil.rect(g, this.x, this.y + this.h - this.footerHeight, this.w, 1, Theme.DIVIDER);
        final int by = this.y + this.h - this.footerHeight + 6;
        final int half = (this.w - 2 * PAD - 4) / 2;
        this.importButton = new int[]{this.x + PAD, by, half, BUTTON_H};
        this.exportButton = new int[]{this.x + PAD + half + 4, by, half, BUTTON_H};
        this.templatesButton = new int[]{this.x + PAD, by + BUTTON_H + 4, this.w - 2 * PAD, BUTTON_H};
        FieldUi.button(g, this.importButton[0], this.importButton[1], this.importButton[2], BUTTON_H, "Import", mx, my);
        FieldUi.button(g, this.exportButton[0], this.exportButton[1], this.exportButton[2], BUTTON_H, "Export", mx, my);
        FieldUi.button(g, this.templatesButton[0], this.templatesButton[1], this.templatesButton[2], BUTTON_H, "Templates", mx, my);
        int hy = this.y + this.h - hintHeight + 8;
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
        if (this.templatesOpen) {
            this.renderTemplates(g, mx, my);
            return;
        }
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
            this.menuItem(g, i, MENU[i], mx, my, i == MENU.length - 1 ? Theme.DANGER : Theme.TEXT, 0);
        }
    }

    private int[] templatesRect() {
        final int rows = MacroTemplates.entries().size();
        final int height = 22 + rows * TEMPLATE_ROW_H + 6;
        final int tx = this.x + this.w + 4;
        final int ty = Math.max(4, Math.min(this.templatesButton[1] + BUTTON_H - height, UiScale.height() - height - 4));
        return new int[]{tx, ty, TEMPLATE_W, height};
    }

    private void renderTemplates(GuiGraphicsExtractor g, int mx, int my) {
        final int[] r = this.templatesRect();
        RenderUtil.shadow(g, r[0], r[1], r[2], r[3], 4);
        RenderUtil.roundedRectWithBorder(g, r[0], r[1], r[2], r[3], 4, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        UiText.draw(g, "Insert a template (added switched off)", r[0] + 10, r[1] + 8, Theme.TEXT_MUTED, UiText.Weight.SEMIBOLD);
        int ry = r[1] + 22;
        for (MacroTemplates.Entry entry : MacroTemplates.entries()) {
            if (RenderUtil.inside(mx, my, r[0] + 3, ry, r[2] - 6, TEMPLATE_ROW_H)) {
                RenderUtil.roundedRect(g, r[0] + 3, ry, r[2] - 6, TEMPLATE_ROW_H, 3, Theme.HOVER_OVERLAY);
            }
            UiText.draw(g, UiText.ellipsize(entry.title(), r[2] - 20, UiText.Weight.SEMIBOLD), r[0] + 10, ry + 5, Theme.TEXT, UiText.Weight.SEMIBOLD);
            UiText.draw(g, UiText.ellipsize(entry.description(), r[2] - 20), r[0] + 10, ry + 18, Theme.TEXT_FAINT);
            ry += TEMPLATE_ROW_H;
        }
    }

    private boolean templatesClicked(double mx, double my) {
        final int[] r = this.templatesRect();
        if (!RenderUtil.inside(mx, my, r[0], r[1], r[2], r[3])) {
            this.closeMenu();
            return true;
        }
        final int index = (int) ((my - r[1] - 22) / TEMPLATE_ROW_H);
        final List<MacroTemplates.Entry> entries = MacroTemplates.entries();
        if (my >= r[1] + 22 && index >= 0 && index < entries.size()) {
            this.closeMenu();
            final MacroTemplates.Entry entry = entries.get(index);
            final Workflow workflow = MacroTemplates.instantiate(entry, this.state.workflows());
            if (workflow == null) {
                this.state.toast("Could not load the template", true);
            } else {
                this.state.history().execute(new WorkflowCommands.Add(workflow, this.state.workflows().size(), "Insert template"));
                this.state.toast("Added \"" + workflow.name + "\" (switched off)", false);
            }
        }
        return true;
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
        this.state.history().execute(new WorkflowCommands.Add(workflow, this.state.workflows().size(), "New macro"));
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
                this.state.history().execute(new WorkflowCommands.Rename(this.renaming, text));
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
        this.state.history().execute(new WorkflowCommands.Add(copy, list.indexOf(workflow) + 1, "Duplicate macro"));
    }

    private void delete(Workflow workflow) {
        this.state.history().execute(new WorkflowCommands.Delete(workflow, this.state.workflows().indexOf(workflow)));
    }

    /** Copies the macro as macro JSON (a "workflows" envelope) to the clipboard. */
    public void export(Workflow workflow) {
        MacroClipboard.exportWorkflow(workflow);
        this.state.toast("Copied \"" + workflow.name + "\" as macro JSON to the clipboard", false);
    }

    /** Adds the macros from the clipboard as new, switched-off macros. Bad content becomes a toast. */
    public void importFromClipboard() {
        final MacroDocument doc;
        try {
            doc = MacroClipboard.read();
        } catch (Exception e) {
            this.state.toast(MacroClipboard.describeFailure(e, "import"), true);
            return;
        }
        final List<Workflow> imported = MacroClipboard.workflowsFor(doc, this.state.workflows());
        if (imported.isEmpty()) {
            this.state.toast("The clipboard has no macros to import", true);
            return;
        }
        final List<EditorCommand> parts = new ArrayList<>();
        int index = this.state.workflows().size();
        for (Workflow workflow : imported) {
            parts.add(new WorkflowCommands.Add(workflow, index++, "Import macro"));
        }
        this.state.history().execute(new CompositeCommand("Import macros", parts));
        this.state.toast("Imported " + imported.size() + (imported.size() == 1 ? " macro" : " macros") + " (switched off)", false);
    }

    // ------------------------------------------------------------------ input

    /** Clicks while the menu is open (always consumed). */
    public boolean menuClicked(double mx, double my) {
        if (this.templatesOpen) {
            return this.templatesClicked(mx, my);
        }
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
            case 2 -> {
                this.closeMenu();
                this.export(workflow);
            }
            case 3 -> this.confirmDelete = true;
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
        if (button == 0) {
            if (RenderUtil.inside(mx, my, this.importButton[0], this.importButton[1], this.importButton[2], this.importButton[3])) {
                OptionWidget.playClick();
                this.importFromClipboard();
                return true;
            }
            if (RenderUtil.inside(mx, my, this.exportButton[0], this.exportButton[1], this.exportButton[2], this.exportButton[3])) {
                OptionWidget.playClick();
                if (this.state.current() != null) {
                    this.export(this.state.current());
                }
                return true;
            }
            if (RenderUtil.inside(mx, my, this.templatesButton[0], this.templatesButton[1], this.templatesButton[2], this.templatesButton[3])) {
                OptionWidget.playClick();
                this.templatesOpen = true;
                return true;
            }
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
                final int target = Math.max(0, Math.min(list.size() - 1, to));
                this.state.history().execute(new WorkflowCommands.Reorder(list.get(from), from, target));
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
