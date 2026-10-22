package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.ui.model.OptionKind;
import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.zone.ZoneEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Enum dropdown ({@link OptionKind#DROPDOWN}) and dynamic zone dropdown ({@link OptionKind#ZONE_DROPDOWN}).
 * The zone dropdown stores the zone ordinal as an Integer and recomputes its entries every frame.
 */
public class DropdownWidget extends CardWidget {

    private static final int BTN_W = 120;
    private static final int BTN_H = 18;
    private static final int ITEM_H = 16;
    private static final int MAX_VISIBLE = 8;

    private record Entry(String label, Object value) {
    }

    private final boolean zoneMode;
    private boolean open;
    private int scroll;

    public DropdownWidget(OptionNode node) {
        super(node);
        this.zoneMode = node.kind() == OptionKind.ZONE_DROPDOWN;
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

    private List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        if (zoneMode) {
            SkyblockLocationEnum loc = SkysensConfig.getInstance().qolCategory.sensitivity.locationEnum.get();
            Class<?> zc = loc == null ? null : loc.getZoneEnum();
            Object[] cs = zc == null ? null : zc.getEnumConstants();
            if (cs != null) {
                for (int i = 0; i < cs.length; i++) {
                    String label = cs[i] instanceof ZoneEnum<?> z ? z.getDisplayName() : String.valueOf(cs[i]);
                    out.add(new Entry(label, Integer.valueOf(i)));
                }
            }
        } else {
            Object[] cs = node.valueType().getEnumConstants();
            if (cs != null) {
                for (Object c : cs) {
                    out.add(new Entry(c.toString(), c));
                }
            }
        }
        return out;
    }

    private int selectedIndex(List<Entry> es) {
        Object cur = node.get();
        if (zoneMode) {
            if (cur instanceof Number n) {
                int i = n.intValue();
                return i >= 0 && i < es.size() ? i : -1;
            }
            return -1;
        }
        for (int i = 0; i < es.size(); i++) {
            if (es.get(i).value() == cur) {
                return i;
            }
        }
        return -1;
    }

    private void choose(Entry e) {
        if (zoneMode) {
            if (!e.value().equals(node.get())) {
                node.set(e.value());
            }
        } else if (e.value() != node.get()) {
            node.set(e.value());
        }
    }

    // ------------------------------------------------------------------ geometry

    private int visibleCount(int n) {
        return Math.min(MAX_VISIBLE, n);
    }

    private int clampScroll(int n) {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, n - visibleCount(n))));
        return scroll;
    }

    /** Popover bounds as {x, y, w, h}. */
    private int[] popover(List<Entry> es) {
        var window = Minecraft.getInstance().getWindow();
        int sw = window.getGuiScaledWidth();
        int sh = window.getGuiScaledHeight();
        int maxLabel = 0;
        for (Entry e : es) {
            maxLabel = Math.max(maxLabel, UiText.width(e.label(), UiText.Weight.SEMIBOLD));
        }
        int w = Math.min(Math.max(BTN_W, maxLabel + 8 + 8 + 12), sw - 8);
        int h = visibleCount(es.size()) * ITEM_H + 4;
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
        List<Entry> es = entries();
        boolean disabled = es.isEmpty();
        if (disabled && open) {
            open = false;
        }
        boolean hover = !disabled && overButton(mouseX, mouseY);
        int border = open ? Theme.ACCENT : hover ? Theme.TEXT_FAINT : Theme.BORDER;
        RenderUtil.inputBox(g, cx, cy, BTN_W, BTN_H, 4, open || hover, border, border);

        String label;
        int color;
        if (disabled) {
            label = zoneMode ? "No zones" : "Select…";
            color = Theme.TEXT_FAINT;
        } else {
            int sel = selectedIndex(es);
            label = sel < 0 ? "Select…" : es.get(sel).label();
            color = sel < 0 ? Theme.TEXT_MUTED : Theme.TEXT;
        }
        label = UiText.ellipsize(label, BTN_W - 12 - 14);
        UiText.draw(g, label, cx + 6, cy + (BTN_H - UiText.lineHeight()) / 2 + 1, color);

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
        if (!open) {
            return;
        }
        List<Entry> es = entries();
        if (es.isEmpty()) {
            open = false;
            return;
        }
        int n = es.size();
        int vis = visibleCount(n);
        int start = clampScroll(n);
        int[] p = popover(es);
        int px = p[0], py = p[1], pw = p[2], ph = p[3];
        int sel = selectedIndex(es);

        RenderUtil.shadow(g, px, py, pw, ph, 5);
        RenderUtil.roundedRectWithBorder(g, px, py, pw, ph, 5, Theme.POPOVER, Theme.POPOVER_BORDER);

        for (int row = 0; row < vis; row++) {
            int idx = start + row;
            Entry e = es.get(idx);
            int iy = py + 2 + row * ITEM_H;
            if (RenderUtil.inside(mouseX, mouseY, px + 2, iy, pw - 4, ITEM_H)) {
                RenderUtil.roundedRect(g, px + 2, iy, pw - 4, ITEM_H, 3, Theme.ACCENT_SOFT);
            }
            boolean selected = idx == sel;
            UiText.Weight weight = selected ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR;
            String text = UiText.ellipsize(e.label(), pw - 8 - 8 - 12, weight);
            UiText.draw(g, text, px + 8, iy + (ITEM_H - UiText.lineHeight()) / 2 + 1,
                    selected ? Theme.ACCENT : Theme.TEXT, weight);
            if (selected) {
                UiText.drawRight(g, "✓", px + pw - 8, iy + (ITEM_H - UiText.lineHeight()) / 2 + 1,
                        Theme.ACCENT);
            }
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
        return open;
    }

    @Override
    public boolean overlayContains(double mx, double my) {
        if (!open) {
            return false;
        }
        List<Entry> es = entries();
        if (es.isEmpty()) {
            return false;
        }
        int[] p = popover(es);
        return RenderUtil.inside(mx, my, p[0], p[1], p[2], p[3]);
    }

    @Override
    public void closeOverlay() {
        open = false;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (open) {
            List<Entry> es = entries();
            if (!es.isEmpty()) {
                int[] p = popover(es);
                if (RenderUtil.inside(mx, my, p[0], p[1], p[2], p[3])) {
                    if (button == 0) {
                        int n = es.size();
                        int row = (int) Math.floor((my - p[1] - 2) / ITEM_H);
                        if (row >= 0 && row < visibleCount(n)) {
                            choose(es.get(clampScroll(n) + row));
                            open = false;
                            playClick();
                        }
                    }
                    return true;
                }
            }
            open = false;
            // clicking the button while open just closes the popover
            return overButton(mx, my);
        }
        if (button == 0 && overButton(mx, my)) {
            List<Entry> es = entries();
            if (es.isEmpty()) {
                return true;
            }
            int n = es.size();
            int sel = selectedIndex(es);
            scroll = sel < 0 ? 0 : sel - visibleCount(n) / 2;
            clampScroll(n);
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
            clampScroll(entries().size());
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
