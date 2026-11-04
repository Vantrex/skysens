package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.model.OptionKind;
import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiScale;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dropdown with a check per entry ({@link OptionKind#MULTI_SELECT}) for a {@code List<SomeEnum>} field holding the
 * checked constants. Clicking an entry toggles it and keeps the popover open; the first row checks or unchecks all.
 * The value is always replaced by a fresh list in enum order, never changed in place: the list may be the shared
 * default instance after a reset.
 */
public class MultiSelectWidget extends CardWidget {

    private static final int BTN_W = 120;
    private static final int BTN_H = 18;
    private static final int ITEM_H = 16;
    private static final int MAX_VISIBLE = 10;
    private static final String ALL_LABEL = "Select all";
    private static final String NONE_LABEL = "Select none";

    private final Object[] constants;
    private boolean open;
    private int scroll;

    public MultiSelectWidget(OptionNode node) {
        super(node);
        this.constants = enumConstants(node);
    }

    /** The constants of the list's element type, from the field's generic type ({@code valueType()} is just List). */
    private static Object[] enumConstants(OptionNode node) {
        if (node.field() != null) {
            Type type = node.field().getGenericType();
            if (type instanceof ParameterizedType p && p.getActualTypeArguments().length == 1
                    && p.getActualTypeArguments()[0] instanceof Class<?> c && c.isEnum()) {
                return c.getEnumConstants();
            }
        }
        return new Object[0];
    }

    @Override
    protected int controlWidth() {
        return BTN_W;
    }

    @Override
    protected int controlHeight() {
        return BTN_H;
    }

    // ------------------------------------------------------------------ data

    private Collection<?> selected() {
        return node.get() instanceof Collection<?> c ? c : List.of();
    }

    private int selectedCount() {
        Collection<?> sel = selected();
        int n = 0;
        for (Object c : constants) {
            if (sel.contains(c)) {
                n++;
            }
        }
        return n;
    }

    private static String label(Object constant) {
        return constant instanceof Enum<?> en ? UiText.enumLabel(en) : String.valueOf(constant);
    }

    /** Rows of the popover: the select all/none row, then one per constant. */
    private int rowCount() {
        return constants.length + 1;
    }

    private void toggle(int index) {
        Collection<?> sel = selected();
        List<Object> next = new ArrayList<>();
        if (index < 0) {
            boolean all = selectedCount() == constants.length;
            if (!all) {
                next.addAll(List.of(constants));
            }
        } else {
            Object target = constants[index];
            for (Object c : constants) {
                boolean checked = sel.contains(c);
                if (c == target ? !checked : checked) {
                    next.add(c);
                }
            }
        }
        node.set(next);
    }

    // ------------------------------------------------------------------ geometry

    private int visibleCount() {
        return Math.min(MAX_VISIBLE, rowCount());
    }

    private int clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rowCount() - visibleCount())));
        return scroll;
    }

    /** Popover bounds as {x, y, w, h}. */
    private int[] popover() {
        int sw = UiScale.width();
        int sh = UiScale.height();
        int maxLabel = UiText.width(NONE_LABEL, UiText.Weight.SEMIBOLD);
        for (Object c : constants) {
            maxLabel = Math.max(maxLabel, UiText.width(label(c), UiText.Weight.SEMIBOLD));
        }
        int w = Math.min(Math.max(BTN_W, maxLabel + 8 + 8 + 14), sw - 8);
        int h = visibleCount() * ITEM_H + 4;
        int px = Math.max(4, Math.min(controlX() + BTN_W - w, sw - w - 4));
        int py = controlY() + BTN_H + 2;
        if (py + h > sh - 4) {
            int above = controlY() - 2 - h;
            py = above >= 4 ? above : Math.max(4, sh - 4 - h);
        }
        return new int[]{px, py, w, h};
    }

    private boolean overButton(double mx, double my) {
        return RenderUtil.inside(mx, my, controlX(), controlY(), BTN_W, BTN_H);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    protected void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY,
                                 float partialTick) {
        boolean disabled = constants.length == 0;
        if (disabled && open) {
            open = false;
        }
        boolean hover = !disabled && overButton(mouseX, mouseY);
        int border = open ? Theme.ACCENT : hover ? Theme.TEXT_FAINT : Theme.BORDER;
        RenderUtil.inputBox(g, cx, cy, BTN_W, BTN_H, 4, open || hover, border, border);

        int count = selectedCount();
        String text;
        if (disabled) {
            text = "Nothing to select";
        } else if (count == constants.length) {
            text = "All (" + count + ")";
        } else if (count == 0) {
            text = "None";
        } else {
            text = count + " of " + constants.length;
        }
        int color = disabled || count == 0 ? Theme.TEXT_MUTED : Theme.TEXT;
        text = UiText.ellipsize(text, BTN_W - 12 - 14);
        UiText.draw(g, text, cx + 6, cy + (BTN_H - UiText.lineHeight()) / 2 + 1, color);

        if (!disabled) {
            // chevron triangle (down when closed, up when open)
            int ax = cx + BTN_W - 11;
            int ay = cy + BTN_H / 2 - 1;
            int c = open ? Theme.ACCENT_HOVER : Theme.TEXT_MUTED;
            for (int i = 0; i < 3; i++) {
                int half = open ? i : 2 - i;
                int row = ay + i - (open ? 1 : 0);
                g.fill(ax + 2 - half, row, ax + 3 + half, row + 1, c);
            }
        }
    }

    @Override
    public void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.renderOverlay(g, mouseX, mouseY, partialTick);
        if (!open || constants.length == 0) {
            return;
        }
        int n = rowCount();
        int vis = visibleCount();
        int start = clampScroll();
        int[] p = popover();
        int px = p[0], py = p[1], pw = p[2], ph = p[3];
        Collection<?> sel = selected();
        boolean all = selectedCount() == constants.length;

        RenderUtil.shadow(g, px, py, pw, ph, 5);
        RenderUtil.roundedRectWithBorder(g, px, py, pw, ph, 5, Theme.POPOVER, Theme.POPOVER_BORDER);

        for (int row = 0; row < vis; row++) {
            int idx = start + row;
            int iy = py + 2 + row * ITEM_H;
            if (RenderUtil.inside(mouseX, mouseY, px + 2, iy, pw - 4, ITEM_H)) {
                RenderUtil.roundedRect(g, px + 2, iy, pw - 4, ITEM_H, 3, Theme.ACCENT_SOFT);
            }
            int ty = iy + (ITEM_H - UiText.lineHeight()) / 2 + 1;
            if (idx == 0) {
                UiText.draw(g, all ? NONE_LABEL : ALL_LABEL, px + 8, ty, Theme.TEXT_MUTED, UiText.Weight.SEMIBOLD);
                continue;
            }
            Object c = constants[idx - 1];
            boolean checked = sel.contains(c);
            UiText.Weight weight = checked ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR;
            String text = UiText.ellipsize(label(c), pw - 8 - 8 - 14, weight);
            UiText.draw(g, text, px + 8, ty, checked ? Theme.ACCENT : Theme.TEXT, weight);
            UiText.drawRight(g, checked ? "✓" : "·", px + pw - 8, ty, checked ? Theme.ACCENT : Theme.TEXT_FAINT);
        }

        if (n > vis) {
            int trackH = ph - 4;
            int thumbH = Math.max(8, trackH * vis / n);
            int range = n - vis;
            int thumbY = py + 2 + (trackH - thumbH) * start / range;
            RenderUtil.roundedRect(g, px + pw - 4, thumbY, 2, thumbH, 1, Theme.TEXT_FAINT);
        }
    }

    // ------------------------------------------------------------------ overlay contract

    @Override
    public boolean hasOverlay() {
        return open || super.hasOverlay();
    }

    @Override
    public boolean overlayContains(double mx, double my) {
        if (super.overlayContains(mx, my)) {
            return true;
        }
        if (!open || constants.length == 0) {
            return false;
        }
        int[] p = popover();
        return RenderUtil.inside(mx, my, p[0], p[1], p[2], p[3]);
    }

    @Override
    public void closeOverlay() {
        super.closeOverlay();
        open = false;
    }

    @Override
    protected void onReset() {
        open = false;
    }

    // ------------------------------------------------------------------ input

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (open) {
            int[] p = popover();
            if (RenderUtil.inside(mx, my, p[0], p[1], p[2], p[3])) {
                if (button == 0) {
                    int row = (int) Math.floor((my - p[1] - 2) / ITEM_H);
                    if (row >= 0 && row < visibleCount()) {
                        // stays open, so several entries can be toggled in a row
                        toggle(clampScroll() + row - 1);
                        playClick();
                    }
                }
                return true;
            }
            open = false;
            // clicking the button while open just closes the popover
            return overButton(mx, my);
        }
        if (button == 0 && overButton(mx, my)) {
            if (constants.length == 0) {
                return true;
            }
            scroll = 0;
            open = true;
            playClick();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (open && overlayContains(mx, my)) {
            scroll -= (int) Math.signum(amount);
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (open && e.isEscape()) {
            open = false;
            return true;
        }
        return false;
    }
}
