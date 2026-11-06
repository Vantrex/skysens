package de.vantrex.skysens.client.features.macro.editor;

import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * The editor's keyboard shortcut list (F1 or the "Keys" header button). A modal card over the dimmed editor; any click,
 * Esc or F1 closes it. The entries are a static list, so a new shortcut only needs a line here.
 */
public final class ShortcutsOverlay {

    private record Entry(String keys, String action) {
    }

    private record Section(String title, List<Entry> entries) {
    }

    private static final List<Section> SECTIONS = List.of(
            new Section("Canvas", List.of(
                    new Entry("Drag empty space", "Select with a box"),
                    new Entry("Middle-drag / Space+drag", "Pan"),
                    new Entry("Wheel / Shift+wheel", "Pan up-down / left-right"),
                    new Entry("Ctrl+wheel", "Zoom at the cursor"),
                    new Entry("Shift+1", "Fit everything into view"),
                    new Entry("0", "Reset zoom to 100%"))),
            new Section("Nodes & wires", List.of(
                    new Entry("Click / Shift+click", "Select / add to selection"),
                    new Entry("Ctrl+A", "Select all"),
                    new Entry("Drag a node", "Move (hold Alt: no grid snap)"),
                    new Entry("Drag from an output", "Connect; drop on empty space to add a node"),
                    new Entry("Drag a connected input", "Pick up the wire"),
                    new Entry("Delete / Backspace", "Delete selected nodes or wire"),
                    new Entry("D", "Disable / enable selected nodes"))),
            new Section("Editing", List.of(
                    new Entry("Ctrl+Z", "Undo"),
                    new Entry("Ctrl+Y / Ctrl+Shift+Z", "Redo"),
                    new Entry("Ctrl+C / Ctrl+X / Ctrl+V", "Copy / cut / paste nodes"),
                    new Entry("Ctrl+D", "Duplicate selected nodes"),
                    new Entry("Ctrl+S", "Save (also saved when closing)"))),
            new Section("Picker & details", List.of(
                    new Entry("Tab", "Open the node picker"),
                    new Entry("Up / Down, Enter", "Choose in the picker"),
                    new Entry("Double-click / Enter", "Open the node's details"),
                    new Entry("Esc", "Close picker, details or editor"),
                    new Entry("F1", "Show this list"))));

    private static final String FOOTNOTE = "Letter shortcuts follow your keyboard layout (e.g. Z on QWERTZ).";
    private static final int PAD = 14;
    private static final int COLUMN_GAP = 22;
    private static final int KEY_GAP = 10;
    private static final int ROW_H = 13;
    private static final int SECTION_GAP = 8;
    private static final int DIM = 0xA0000000;

    private boolean open;

    public boolean isOpen() {
        return this.open;
    }

    public void open() {
        this.open = true;
    }

    public void close() {
        this.open = false;
    }

    public void toggle() {
        this.open = !this.open;
    }

    public void render(GuiGraphicsExtractor g, int sw, int sh) {
        if (!this.open) {
            return;
        }
        RenderUtil.rect(g, 0, 0, sw, sh, DIM);

        // two columns: the first two sections left, the rest right
        int keysW = 0;
        int actionW = 0;
        for (Section section : SECTIONS) {
            for (Entry entry : section.entries()) {
                keysW = Math.max(keysW, UiText.width(entry.keys(), UiText.Weight.SEMIBOLD));
                actionW = Math.max(actionW, UiText.width(entry.action()));
            }
        }
        final int columnW = keysW + KEY_GAP + actionW;
        final int split = SECTIONS.size() / 2;
        final int leftH = columnHeight(0, split);
        final int rightH = columnHeight(split, SECTIONS.size());
        final int cardW = Math.min(sw - 16, 2 * PAD + 2 * columnW + COLUMN_GAP);
        final int cardH = PAD + 12 + 10 + Math.max(leftH, rightH) + 6 + 10 + PAD;
        final int cx = sw / 2 - cardW / 2;
        final int cy = Math.max(8, sh / 2 - cardH / 2);

        RenderUtil.shadow(g, cx, cy, cardW, cardH, 6);
        RenderUtil.roundedRectWithBorder(g, cx, cy, cardW, cardH, 6, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        UiText.draw(g, "Keyboard shortcuts", cx + PAD, cy + PAD, Theme.TEXT, UiText.Weight.SEMIBOLD);
        UiText.drawRight(g, "Esc / F1 to close", cx + cardW - PAD, cy + PAD, Theme.TEXT_FAINT);

        final int top = cy + PAD + 12 + 10;
        this.renderColumn(g, 0, split, cx + PAD, top, keysW);
        this.renderColumn(g, split, SECTIONS.size(), cx + PAD + columnW + COLUMN_GAP, top, keysW);
        UiText.draw(g, FOOTNOTE, cx + PAD, cy + cardH - PAD - 9, Theme.TEXT_FAINT);
    }

    private static int columnHeight(int from, int to) {
        int h = 0;
        for (int i = from; i < to; i++) {
            if (i > from) {
                h += SECTION_GAP;
            }
            h += 12 + SECTIONS.get(i).entries().size() * ROW_H;
        }
        return h;
    }

    private void renderColumn(GuiGraphicsExtractor g, int from, int to, int x, int y, int keysW) {
        for (int i = from; i < to; i++) {
            final Section section = SECTIONS.get(i);
            if (i > from) {
                y += SECTION_GAP;
            }
            UiText.draw(g, section.title(), x, y, Theme.ACCENT, UiText.Weight.SEMIBOLD);
            y += 12;
            for (Entry entry : section.entries()) {
                UiText.draw(g, entry.keys(), x, y, Theme.TEXT, UiText.Weight.SEMIBOLD);
                UiText.draw(g, entry.action(), x + keysW + KEY_GAP, y, Theme.TEXT_MUTED);
                y += ROW_H;
            }
        }
    }

}
