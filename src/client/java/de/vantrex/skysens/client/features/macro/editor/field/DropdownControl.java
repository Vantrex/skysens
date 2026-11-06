package de.vantrex.skysens.client.features.macro.editor.field;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A dropdown button with a popover list, single or multi select. Only one popover is open at a time ({@link #active()});
 * the editor draws it last and gives it every click, key and wheel event first. Lists longer than
 * {@value #FILTER_FROM} entries get a type-to-filter box. A stored value that is no longer a choice (stale enum
 * name) is shown in red instead of crashing.
 */
public final class DropdownControl {

    public static final int HEIGHT = 20;
    private static final int ITEM_H = 16;
    private static final int MAX_VISIBLE = 8;
    private static final int FILTER_FROM = 8;
    private static final int FILTER_H = 22;

    private static @Nullable DropdownControl open;

    private final Supplier<List<Choice>> choices;
    private final boolean multi;
    private final Supplier<String> single;
    private final Consumer<String> onPick;
    private final Supplier<List<String>> selected;
    private final Consumer<List<String>> onPickMulti;
    private final String emptyLabel;
    private final SearchField filter = new SearchField(text -> { }).placeholder("Filter…").noHint().maxLength(32);

    private int x;
    private int y;
    private int w;
    private int scroll;
    private int hover = -1;
    private int lastMx = -1;
    private int lastMy = -1;
    private List<Choice> shown = List.of();
    private int[] popover = new int[4];

    private DropdownControl(Supplier<List<Choice>> choices, boolean multi, Supplier<String> single, Consumer<String> onPick,
                            Supplier<List<String>> selected, Consumer<List<String>> onPickMulti, String emptyLabel) {
        this.choices = choices;
        this.multi = multi;
        this.single = single;
        this.onPick = onPick;
        this.selected = selected;
        this.onPickMulti = onPickMulti;
        this.emptyLabel = emptyLabel;
    }

    public static DropdownControl single(Supplier<List<Choice>> choices, Supplier<String> current, Consumer<String> onPick) {
        return new DropdownControl(choices, false, current, onPick, List::of, v -> { }, "Choose…");
    }

    public static DropdownControl multi(Supplier<List<Choice>> choices, Supplier<List<String>> selected, Consumer<List<String>> onPick,
                                        String emptyLabel) {
        return new DropdownControl(choices, true, () -> "", v -> { }, selected, onPick, emptyLabel);
    }

    public static @Nullable DropdownControl active() {
        return open;
    }

    public static void closeActive() {
        if (open != null) {
            open.filter.setFocused(false);
        }
        open = null;
    }

    public boolean isOpen() {
        return open == this;
    }

    // ------------------------------------------------------------------ rendering

    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        this.x = x;
        this.y = y;
        this.w = w;
        final boolean hover = RenderUtil.inside(mx, my, x, y, w, HEIGHT) && open == null;
        final boolean isOpen = this.isOpen();
        final int border = isOpen ? Theme.ACCENT : hover ? Theme.TEXT_FAINT : Theme.BORDER;
        RenderUtil.inputBox(g, x, y, w, HEIGHT, 4, isOpen || hover, border, border);
        final String label = this.buttonLabel();
        final boolean bad = this.hasUnknown();
        final int color = bad ? Theme.DANGER : label.equals(this.emptyLabel) ? Theme.TEXT_FAINT : Theme.TEXT;
        UiText.draw(g, UiText.ellipsize(label, w - 28), x + 6, y + (HEIGHT - 9) / 2 + 1, color);
        final int ax = x + w - 11;
        final int ay = y + HEIGHT / 2;
        for (int i = 0; i < 4; i++) {
            RenderUtil.rect(g, ax + i, ay - 2 + i, 7 - 2 * i, 1, Theme.TEXT_MUTED);
        }
    }

    private String buttonLabel() {
        final List<Choice> all = this.choices.get();
        if (this.multi) {
            final List<String> values = this.selected.get();
            if (values.isEmpty()) {
                return this.emptyLabel;
            }
            final List<String> labels = new ArrayList<>();
            for (String value : values) {
                labels.add(labelOf(all, value));
            }
            return String.join(", ", labels);
        }
        final String current = this.single.get();
        if (current == null || current.isEmpty()) {
            return this.emptyLabel;
        }
        for (Choice c : all) {
            if (c.value().equals(current)) {
                return c.label();
            }
        }
        return current + " (unknown)";
    }

    private boolean hasUnknown() {
        final List<Choice> all = this.choices.get();
        if (this.multi) {
            for (String value : this.selected.get()) {
                if (all.stream().noneMatch(c -> c.value().equals(value))) {
                    return true;
                }
            }
            return false;
        }
        final String current = this.single.get();
        return current != null && !current.isEmpty() && all.stream().noneMatch(c -> c.value().equals(current));
    }

    private static String labelOf(List<Choice> all, String value) {
        for (Choice c : all) {
            if (c.value().equals(value)) {
                return c.label();
            }
        }
        return value + " (unknown)";
    }

    private boolean hasFilter() {
        return this.choices.get().size() > FILTER_FROM;
    }

    private void refilter() {
        final String query = this.filter.text().toLowerCase(java.util.Locale.ROOT);
        final List<Choice> out = new ArrayList<>();
        for (Choice c : this.choices.get()) {
            if (query.isEmpty() || c.label().toLowerCase(java.util.Locale.ROOT).contains(query)) {
                out.add(c);
            }
        }
        this.shown = out;
    }

    /** Draws the popover; call last (outside any scissor) when {@link #active()} is this control. */
    public void renderPopover(GuiGraphicsExtractor g, int mx, int my) {
        this.refilter();
        final boolean filterBox = this.hasFilter();
        final int visible = Math.min(MAX_VISIBLE, Math.max(1, this.shown.size()));
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, this.shown.size() - visible)));
        int widest = 0;
        for (Choice c : this.shown) {
            widest = Math.max(widest, UiText.width(c.label(), UiText.Weight.SEMIBOLD));
        }
        final int sw = UiScale.width();
        final int sh = UiScale.height();
        final int pw = Math.min(Math.max(this.w, widest + 36), sw - 8);
        final int ph = visible * ITEM_H + 4 + (filterBox ? FILTER_H : 0);
        final int px = Math.max(4, Math.min(this.x, sw - pw - 4));
        int py = this.y + HEIGHT + 2;
        if (py + ph > sh - 4) {
            final int above = this.y - 2 - ph;
            py = above >= 4 ? above : Math.max(4, sh - 4 - ph);
        }
        this.popover = new int[]{px, py, pw, ph};

        RenderUtil.shadow(g, px, py, pw, ph, 4);
        RenderUtil.roundedRectWithBorder(g, px, py, pw, ph, 4, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        int listTop = py + 2;
        if (filterBox) {
            this.filter.layout(px + 4, py + 3, pw - 8, FILTER_H - 5);
            this.filter.render(g, mx, my);
            listTop += FILTER_H;
        }
        final boolean moved = mx != this.lastMx || my != this.lastMy;
        this.lastMx = mx;
        this.lastMy = my;
        RenderUtil.scissor(g, px, listTop, pw, visible * ITEM_H);
        if (this.shown.isEmpty()) {
            UiText.draw(g, "No matches", px + 8, listTop + 4, Theme.TEXT_FAINT);
        }
        final Set<String> picked = this.multi ? new LinkedHashSet<>(this.selected.get()) : Set.of();
        final String current = this.single.get();
        for (int i = 0; i < visible && i + this.scroll < this.shown.size(); i++) {
            final Choice c = this.shown.get(i + this.scroll);
            final int iy = listTop + i * ITEM_H;
            final boolean over = RenderUtil.inside(mx, my, px + 2, iy, pw - 4, ITEM_H);
            if (over && moved) {
                this.hover = i + this.scroll;
            }
            final boolean on = this.multi ? picked.contains(c.value()) : c.value().equals(current);
            if (i + this.scroll == this.hover) {
                RenderUtil.roundedRect(g, px + 2, iy, pw - 4, ITEM_H, 3, Theme.HOVER_OVERLAY);
            }
            if (this.multi) {
                RenderUtil.roundedRectWithBorder(g, px + 7, iy + 3, 10, 10, 2, on ? Theme.ACCENT : Theme.INPUT, on ? Theme.ACCENT : Theme.BORDER);
                if (on) {
                    RenderUtil.rect(g, px + 10, iy + 8, 4, 1, 0xFFFFFFFF);
                    RenderUtil.rect(g, px + 12, iy + 6, 1, 4, 0xFFFFFFFF);
                }
            }
            UiText.draw(g, UiText.ellipsize(c.label(), pw - (this.multi ? 28 : 16)), px + (this.multi ? 22 : 8), iy + (ITEM_H - 9) / 2 + 1,
                    !this.multi && on ? Theme.ACCENT : Theme.TEXT);
        }
        RenderUtil.unscissor(g);
        if (this.shown.size() > visible) {
            final int trackH = visible * ITEM_H;
            final int thumbH = Math.max(10, trackH * visible / this.shown.size());
            final int thumbY = listTop + (trackH - thumbH) * this.scroll / (this.shown.size() - visible);
            RenderUtil.rect(g, px + pw - 4, thumbY, 2, thumbH, Theme.TEXT_FAINT);
        }
    }

    // ------------------------------------------------------------------ input

    /** A click on the button while no popover is open. Returns true if it hit the button. */
    public boolean mouseClicked(double mx, double my) {
        if (!RenderUtil.inside(mx, my, this.x, this.y, this.w, HEIGHT)) {
            return false;
        }
        closeActive();
        open = this;
        this.scroll = 0;
        this.hover = -1;
        this.filter.setText("");
        this.filter.setFocused(this.hasFilter());
        return true;
    }

    /** Handles any click while this popover is open (always consumed). */
    public void popoverClicked(double mx, double my) {
        if (RenderUtil.inside(mx, my, this.x, this.y, this.w, HEIGHT)) {
            closeActive();
            return;
        }
        if (!RenderUtil.inside(mx, my, this.popover[0], this.popover[1], this.popover[2], this.popover[3])) {
            closeActive();
            return;
        }
        if (this.hasFilter() && my < this.popover[1] + FILTER_H) {
            this.filter.mouseClicked(mx, my);
            return;
        }
        final int listTop = this.popover[1] + 2 + (this.hasFilter() ? FILTER_H : 0);
        final int index = this.scroll + (int) ((my - listTop) / ITEM_H);
        if (my >= listTop && index >= 0 && index < this.shown.size()) {
            this.pick(this.shown.get(index));
        }
    }

    private void pick(Choice choice) {
        if (this.multi) {
            final List<String> values = new ArrayList<>(this.selected.get());
            if (!values.remove(choice.value())) {
                values.add(choice.value());
            }
            this.onPickMulti.accept(values);
        } else {
            this.onPick.accept(choice.value());
            closeActive();
        }
    }

    public void mouseScrolled(double mx, double my, double scrollY) {
        if (RenderUtil.inside(mx, my, this.popover[0], this.popover[1], this.popover[2], this.popover[3])) {
            this.scroll -= (int) Math.signum(scrollY) * 2;
        }
    }

    public boolean keyPressed(KeyEvent e) {
        switch (e.key()) {
            case InputConstants.KEY_ESCAPE -> closeActive();
            case InputConstants.KEY_DOWN -> this.move(1);
            case InputConstants.KEY_UP -> this.move(-1);
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                if (this.hover >= 0 && this.hover < this.shown.size()) {
                    this.pick(this.shown.get(this.hover));
                } else if (!this.multi) {
                    closeActive();
                }
            }
            default -> this.filter.keyPressed(e);
        }
        return true;
    }

    private void move(int direction) {
        if (this.shown.isEmpty()) {
            return;
        }
        final int next = Math.max(0, Math.min(this.shown.size() - 1, (this.hover < 0 ? (direction > 0 ? -1 : this.shown.size()) : this.hover) + direction));
        this.hover = next;
        if (next < this.scroll) {
            this.scroll = next;
        } else if (next >= this.scroll + MAX_VISIBLE) {
            this.scroll = next - MAX_VISIBLE + 1;
        }
    }

    public boolean charTyped(CharacterEvent e) {
        return this.filter.charTyped(e);
    }

}
