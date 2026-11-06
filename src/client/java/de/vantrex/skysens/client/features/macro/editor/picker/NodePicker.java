package de.vantrex.skysens.client.features.macro.editor.picker;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ConditionTypes;
import de.vantrex.skysens.client.features.macro.editor.canvas.NodeRenderer;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.ui.SearchField;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiScale;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * The node picker popover: a search box on top and the node types grouped as Triggers, Logic, Conditions and Actions.
 * A condition entry creates an If node pre-filled with that one rule. Arrow keys move, Enter chooses, Esc closes.
 * Opened with Tab, the "+" button or by dropping a wire on empty canvas (then only nodes with an input are listed).
 */
public final class NodePicker {

    /** Where the new node goes and which output it should be wired from. */
    public record Request(double worldX, double worldY, @Nullable String fromNode, @Nullable String fromPort) {
    }

    /** One pickable thing: a node type or a condition shortcut. */
    public record Entry(String group, String title, String subtitle, int color, String icon,
                        @Nullable NodeType node, @Nullable ConditionType condition) {
    }

    private static final int WIDTH = 260;
    private static final int MAX_HEIGHT = 330;
    private static final int SEARCH_H = 24;
    private static final int ROW_H = 24;
    private static final int HEADER_H = 16;
    private static final String[] GROUPS = {"Triggers", "Logic", "Conditions", "Actions"};

    private final SearchField search = new SearchField(text -> this.refilter()).placeholder("Search nodes…").noHint();
    private boolean open;
    private Request request = new Request(0, 0, null, null);
    private boolean needInput;
    private BiConsumer<Entry, Request> onChoose = (e, r) -> { };
    private List<Entry> shown = List.of();
    private int selected;
    private int scroll;
    private int x;
    private int y;
    private int w = WIDTH;
    private int h;
    private int listTop;

    public boolean isOpen() {
        return this.open;
    }

    public void setOnChoose(BiConsumer<Entry, Request> onChoose) {
        this.onChoose = onChoose;
    }

    public void open(int anchorX, int anchorY, boolean needInput, Request request) {
        this.open = true;
        this.request = request;
        this.needInput = needInput;
        this.x = anchorX;
        this.y = anchorY;
        this.search.setText("");
        this.search.setFocused(true);
        this.scroll = 0;
        this.selected = 0;
        this.refilter();
    }

    public void close() {
        this.open = false;
        this.search.setFocused(false);
    }

    private static List<Entry> allEntries() {
        final List<Entry> out = new ArrayList<>();
        for (NodeType type : NodeTypes.all()) {
            final String group = switch (type.category()) {
                case TRIGGER -> "Triggers";
                case LOGIC -> "Logic";
                case ACTION -> "Actions";
                default -> null;
            };
            if (group != null) {
                out.add(new Entry(group, type.displayName(), describe(type), NodeRenderer.categoryColor(type.category()), type.icon(), type, null));
            }
        }
        for (ConditionType condition : ConditionTypes.all()) {
            out.add(new Entry("Conditions", "If " + condition.displayName(), "If node with a " + condition.group().toLowerCase(Locale.ROOT) + " rule",
                    NodeRenderer.categoryColor(NodeCategory.CONDITION), "?", null, condition));
        }
        return out;
    }

    private static String describe(NodeType type) {
        return switch (type.category()) {
            case TRIGGER -> "Starts the macro";
            case LOGIC -> "Splits the path by conditions";
            default -> "Does something";
        };
    }

    private void refilter() {
        final String query = this.search.text().trim().toLowerCase(Locale.ROOT);
        final List<Entry> out = new ArrayList<>();
        for (String group : GROUPS) {
            for (Entry entry : allEntries()) {
                if (!entry.group().equals(group)) {
                    continue;
                }
                // a wire needs a node with an input; triggers have none, and a condition shortcut is an If node
                if (this.needInput && entry.node() != null && entry.node().inputs(new de.vantrex.skysens.client.features.macro.model.MacroNode()).isEmpty()) {
                    continue;
                }
                if (query.isEmpty() || (entry.title() + " " + entry.group() + " " + entry.subtitle()).toLowerCase(Locale.ROOT).contains(query)) {
                    out.add(entry);
                }
            }
        }
        this.shown = out;
        this.selected = Math.max(0, Math.min(this.selected, out.size() - 1));
        this.scroll = 0;
    }

    // ------------------------------------------------------------------ rendering

    /** Rows: -1 index = group header before the entry at this position. */
    private int contentHeight() {
        int hgt = 0;
        String last = null;
        for (Entry entry : this.shown) {
            if (!entry.group().equals(last)) {
                hgt += HEADER_H;
                last = entry.group();
            }
            hgt += ROW_H;
        }
        return hgt;
    }

    public void render(GuiGraphicsExtractor g, int mx, int my) {
        if (!this.open) {
            return;
        }
        final int sw = UiScale.width();
        final int sh = UiScale.height();
        final int content = Math.max(ROW_H, this.contentHeight());
        this.h = Math.min(MAX_HEIGHT, SEARCH_H + 10 + content);
        this.h = Math.min(this.h, sh - 8);
        int px = Math.max(4, Math.min(this.x, sw - this.w - 4));
        int py = Math.max(4, Math.min(this.y, sh - this.h - 4));
        this.x = px;
        this.y = py;
        RenderUtil.shadow(g, px, py, this.w, this.h, 6);
        RenderUtil.roundedRectWithBorder(g, px, py, this.w, this.h, 6, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        this.search.layout(px + 6, py + 6, this.w - 12, SEARCH_H - 4);
        this.search.render(g, mx, my);
        this.listTop = py + SEARCH_H + 6;
        final int viewH = this.h - SEARCH_H - 10;
        final int max = Math.max(0, content - viewH);
        this.scroll = Math.max(0, Math.min(this.scroll, max));
        this.ensureSelectedVisible(viewH);

        RenderUtil.scissor(g, px, this.listTop, this.w, viewH);
        if (this.shown.isEmpty()) {
            UiText.draw(g, "No matching nodes", px + 12, this.listTop + 6, Theme.TEXT_FAINT);
        }
        int cy = this.listTop - this.scroll;
        String last = null;
        for (int i = 0; i < this.shown.size(); i++) {
            final Entry entry = this.shown.get(i);
            if (!entry.group().equals(last)) {
                UiText.draw(g, entry.group().toUpperCase(Locale.ROOT), px + 12, cy + 4, Theme.TEXT_FAINT, UiText.Weight.SEMIBOLD);
                cy += HEADER_H;
                last = entry.group();
            }
            if (cy + ROW_H >= this.listTop && cy <= this.listTop + viewH) {
                final boolean hover = RenderUtil.inside(mx, my, px + 4, cy, this.w - 8, ROW_H) && my >= this.listTop && my < this.listTop + viewH;
                if (hover && (mx != this.lastMx || my != this.lastMy)) {
                    this.selected = i;
                }
                if (i == this.selected) {
                    RenderUtil.roundedRect(g, px + 4, cy, this.w - 8, ROW_H - 1, 4, Theme.ACCENT_SOFT);
                }
                RenderUtil.roundedRect(g, px + 8, cy + 3, 18, ROW_H - 7, 4, entry.color());
                UiText.drawCentered(g, entry.icon(), px + 17, cy + (ROW_H - 9) / 2, 0xFFFFFFFF, UiText.Weight.SEMIBOLD);
                UiText.draw(g, UiText.ellipsize(entry.title(), 90, UiText.Weight.SEMIBOLD), px + 32, cy + (ROW_H - 9) / 2, Theme.TEXT, UiText.Weight.SEMIBOLD);
                UiText.drawRight(g, UiText.ellipsize(entry.subtitle(), this.w - 150), px + this.w - 12, cy + (ROW_H - 9) / 2, Theme.TEXT_FAINT);
            }
            cy += ROW_H;
        }
        RenderUtil.unscissor(g);
        this.lastMx = mx;
        this.lastMy = my;
    }

    private int entryTop(int index) {
        int top = 0;
        String last = null;
        for (int i = 0; i <= index && i < this.shown.size(); i++) {
            if (!this.shown.get(i).group().equals(last)) {
                top += HEADER_H;
                last = this.shown.get(i).group();
            }
            if (i < index) {
                top += ROW_H;
            }
        }
        return top;
    }

    private void ensureSelectedVisible(int viewH) {
        if (this.shown.isEmpty() || !this.keyNav) {
            return;
        }
        final int top = this.entryTop(this.selected);
        if (top - HEADER_H < this.scroll) {
            this.scroll = Math.max(0, top - HEADER_H);
        } else if (top + ROW_H > this.scroll + viewH) {
            this.scroll = top + ROW_H - viewH;
        }
        this.keyNav = false;
    }

    private boolean keyNav;
    private int lastMx = -1;
    private int lastMy = -1;

    // ------------------------------------------------------------------ input

    public boolean contains(double mx, double my) {
        return RenderUtil.inside(mx, my, this.x, this.y, this.w, this.h);
    }

    /** A click while open: chooses the hovered entry, or closes when outside. Always consumed. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!this.open) {
            return false;
        }
        if (!this.contains(mx, my)) {
            this.close();
            return true;
        }
        if (this.search.isOver(mx, my)) {
            this.search.mouseClicked(mx, my);
            return true;
        }
        if (button == 0 && my >= this.listTop) {
            int cy = this.listTop - this.scroll;
            String last = null;
            for (Entry entry : this.shown) {
                if (!entry.group().equals(last)) {
                    cy += HEADER_H;
                    last = entry.group();
                }
                if (my >= cy && my < cy + ROW_H) {
                    this.choose(entry);
                    return true;
                }
                cy += ROW_H;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!this.open) {
            return false;
        }
        this.scroll -= (int) (scrollY * 24);
        return true;
    }

    public boolean keyPressed(KeyEvent e) {
        if (!this.open) {
            return false;
        }
        switch (e.key()) {
            case InputConstants.KEY_ESCAPE -> this.close();
            case InputConstants.KEY_DOWN -> {
                this.selected = Math.min(this.shown.size() - 1, this.selected + 1);
                this.keyNav = true;
            }
            case InputConstants.KEY_UP -> {
                this.selected = Math.max(0, this.selected - 1);
                this.keyNav = true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                if (!this.shown.isEmpty()) {
                    this.choose(this.shown.get(Math.max(0, Math.min(this.selected, this.shown.size() - 1))));
                }
            }
            default -> this.search.keyPressed(e);
        }
        return true;
    }

    public boolean charTyped(CharacterEvent e) {
        return this.open && this.search.charTyped(e);
    }

    private void choose(Entry entry) {
        final Request req = this.request;
        this.close();
        this.onChoose.accept(entry, req);
    }

}
