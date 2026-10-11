package de.vantrex.skysens.client.config.ui;

import de.vantrex.skysens.client.config.ui.render.Animated;
import de.vantrex.skysens.client.config.ui.render.FontFamily;
import de.vantrex.skysens.client.config.ui.render.RenderUtil;
import de.vantrex.skysens.client.config.ui.render.Theme;
import de.vantrex.skysens.client.config.ui.render.ThemePreset;
import de.vantrex.skysens.client.config.ui.render.UiText;
import de.vantrex.skysens.client.config.ui.widget.OptionWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Header button (a small palette glyph) that opens a popover listing the theme presets with mini previews.
 * Selecting a row switches the theme live (with colour crossfade). Rendered in the screen's overlay pass and
 * routed like a dropdown popover: clicks outside close it, ESC closes it.
 * <p>
 * The "Font" row opens a flyout submenu (hover or click) to the side of the popover listing all font families,
 * each rendered in its own font. The flyout is drawn and hit-tested as part of the popover overlay: it stays open
 * while the mouse is over the Font row or the flyout (plus a short grace delay), ESC closes it before the popover.
 */
public final class ThemePicker {

    public static final int BUTTON_SIZE = 18;

    private static final int POP_W = 150;
    private static final int PAD = 4;
    private static final int ROW_H = 26;
    private static final int PREVIEW_W = 28;
    private static final int PREVIEW_H = 18;
    private static final int SECTION_H = 5;
    private static final int FONT_ROW_H = 24;

    private static final int FLY_W = 150;
    private static final int FLY_ROW_H = 20;
    private static final int FLY_HEAD_H = 15;
    private static final int GRACE_MS = 150;
    private static final float SLIDE_PX = 6f;

    /** Flyout entries (group headers + families) in display order; built once. */
    private record FlyItem(FontFamily.Group header, FontFamily family, int y, int h) {
    }

    private static final List<FlyItem> FLY_ITEMS = new ArrayList<>();
    private static final int FLY_CONTENT_H;

    static {
        int y = PAD;
        FontFamily.Group last = null;
        for (FontFamily f : FontFamily.values()) {
            if (f.group() != last) {
                last = f.group();
                FLY_ITEMS.add(new FlyItem(last, null, y, FLY_HEAD_H));
                y += FLY_HEAD_H;
            }
            FLY_ITEMS.add(new FlyItem(null, f, y, FLY_ROW_H));
            y += FLY_ROW_H;
        }
        FLY_CONTENT_H = y + PAD;
    }

    private int bx, by;
    private int screenW = 400, screenH = 300;
    private boolean open;

    private boolean flyOpen;
    private final Animated flyAnim = new Animated(0f, 120f);
    private long leaveSince = -1;
    private float flyScroll;
    /** False after ESC closed the flyout while the mouse rests on the Font row; re-armed once the mouse leaves it. */
    private boolean hoverArmed = true;

    public void layout(int x, int y, int screenW, int screenH) {
        this.bx = x;
        this.by = y;
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        closeFlyout();
    }

    public void toggle() {
        open = !open;
        if (!open) {
            closeFlyout();
        }
    }

    private void closeFlyout() {
        flyOpen = false;
        hoverArmed = true;
        flyAnim.target(0f);
        leaveSince = -1;
    }

    private void openFlyout() {
        if (!flyOpen) {
            flyOpen = true;
            flyAnim.target(1f);
            flyScroll = 0f;
        }
        leaveSince = -1;
    }

    /** ESC: closes the flyout first, then the popover. Returns true when something was closed. */
    public boolean escape() {
        if (flyOpen) {
            closeFlyout();
            hoverArmed = false; // don't reopen right away if the mouse is still over the Font row
            return true;
        }
        if (open) {
            close();
            return true;
        }
        return false;
    }

    public boolean overButton(double mx, double my) {
        return RenderUtil.inside(mx, my, bx, by, BUTTON_SIZE, BUTTON_SIZE);
    }

    // ------------------------------------------------------------------ geometry

    private int popH() {
        return ThemePreset.values().length * ROW_H + SECTION_H + FONT_ROW_H + PAD * 2;
    }

    private int popW() {
        return Math.min(POP_W, screenW - 8);
    }

    private int popX() {
        return Math.max(4, Math.min(bx + BUTTON_SIZE - popW(), screenW - popW() - 4));
    }

    private int popY() {
        int h = popH();
        int below = by + BUTTON_SIZE + 3;
        if (below + h <= screenH - 4) {
            return below;
        }
        int above = by - 3 - h;
        return above >= 4 ? above : Math.max(4, screenH - 4 - h);
    }

    private int fontSectionY(int popY) {
        return popY + PAD + ThemePreset.values().length * ROW_H;
    }

    private int fontRowY(int popY) {
        return fontSectionY(popY) + SECTION_H;
    }

    private boolean overFontRow(double mx, double my) {
        return RenderUtil.inside(mx, my, popX() + 2, fontRowY(popY()), popW() - 4, FONT_ROW_H);
    }

    private int flyH() {
        return Math.min(FLY_CONTENT_H, screenH - 8);
    }

    /** True when the flyout opens to the left of the popover (not enough room on the right). */
    private boolean flyLeft() {
        return popX() + popW() - 1 + FLY_W > screenW - 4;
    }

    private int flyX() {
        // 1px overlap with the popover keeps the two gap-free, so the mouse never leaves both when crossing over
        int x = flyLeft() ? popX() - FLY_W + 1 : popX() + popW() - 1;
        // no room on either side (narrow screen): keep it on screen, overlapping the popover
        return Math.max(4, Math.min(x, screenW - 4 - FLY_W));
    }

    private int flyY() {
        int y = fontRowY(popY()) - PAD;
        return Math.max(4, Math.min(y, screenH - 4 - flyH()));
    }

    private float maxFlyScroll() {
        return Math.max(0, FLY_CONTENT_H - flyH());
    }

    private boolean overFlyout(double mx, double my) {
        return flyOpen && RenderUtil.inside(mx, my, flyX(), flyY(), FLY_W, flyH());
    }

    public boolean overlayContains(double mx, double my) {
        return open && (RenderUtil.inside(mx, my, popX(), popY(), popW(), popH()) || overFlyout(mx, my));
    }

    // ------------------------------------------------------------------ rendering

    /** Draws the header button (inside the normal content pass). */
    public void renderButton(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean hover = overButton(mouseX, mouseY);
        RenderUtil.inputBox(g, bx, by, BUTTON_SIZE, BUTTON_SIZE, 4, open || hover, open ? Theme.ACCENT : Theme.TEXT_FAINT,
                Theme.BORDER);
        // palette glyph: three dots
        int dot = 4;
        int ox = bx + (BUTTON_SIZE - 11) / 2;
        int oy = by + (BUTTON_SIZE - 10) / 2;
        RenderUtil.roundedRect(g, ox, oy, dot, dot, 2, Theme.ACCENT);
        RenderUtil.roundedRect(g, ox + 7, oy, dot, dot, 2, Theme.ACCENT_HOVER);
        RenderUtil.roundedRect(g, ox + 3, oy + 6, dot, dot, 2, hover || open ? Theme.TEXT : Theme.TEXT_MUTED);
    }

    public void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        int px = popX(), py = popY(), pw = popW(), ph = popH();
        RenderUtil.shadow(g, px, py, pw, ph, 5);
        RenderUtil.roundedRectWithBorder(g, px, py, pw, ph, 5, Theme.POPOVER, Theme.POPOVER_BORDER);

        ThemePreset[] presets = ThemePreset.values();
        for (int i = 0; i < presets.length; i++) {
            ThemePreset preset = presets[i];
            int ry = py + PAD + i * ROW_H;
            boolean active = preset == Theme.preset();
            boolean hovered = RenderUtil.inside(mouseX, mouseY, px + 2, ry, pw - 4, ROW_H);
            if (hovered) {
                RenderUtil.roundedRect(g, px + 2, ry, pw - 4, ROW_H, 3, Theme.ACCENT_SOFT);
            }
            drawPreview(g, preset, px + 8, ry + (ROW_H - PREVIEW_H) / 2);
            int tx = px + 8 + PREVIEW_W + 8;
            UiText.Weight weight = active ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR;
            String name = UiText.ellipsize(preset.displayName(), pw - (tx - px) - 22, weight);
            UiText.draw(g, name, tx, ry + (ROW_H - UiText.lineHeight()) / 2 + 1,
                    active ? Theme.ACCENT : Theme.TEXT, weight);
            if (active) {
                UiText.drawRight(g, "✓", px + pw - 9, ry + (ROW_H - UiText.lineHeight()) / 2 + 1, Theme.ACCENT);
            }
        }

        // font row: label + active family (in its own font) + chevron
        int secY = fontSectionY(py);
        RenderUtil.rect(g, px + 6, secY + 2, pw - 12, 1, Theme.POPOVER_BORDER);
        int fy = fontRowY(py);
        if (flyOpen || RenderUtil.inside(mouseX, mouseY, px + 2, fy, pw - 4, FONT_ROW_H)) {
            RenderUtil.roundedRect(g, px + 2, fy, pw - 4, FONT_ROW_H, 3, Theme.ACCENT_SOFT);
        }
        int ty = fy + (FONT_ROW_H - UiText.lineHeight()) / 2 + 1;
        UiText.draw(g, "Font", px + 8, ty, Theme.TEXT);
        UiText.drawRight(g, "›", px + pw - 9, ty, Theme.TEXT_MUTED);
        int nameRight = px + pw - 9 - UiText.width("›") - 5;
        String famName = UiText.ellipsize(UiText.family().displayName(),
                nameRight - (px + 8 + UiText.width("Font")) - 8);
        UiText.drawRight(g, famName, nameRight, ty, Theme.ACCENT);

        renderFlyout(g, mouseX, mouseY);
    }

    /** Hover/grace state machine; runs every frame while the popover is open. */
    private void updateFlyout(int mouseX, int mouseY) {
        boolean overRow = overFontRow(mouseX, mouseY);
        if (!flyOpen) {
            if (!overRow) {
                hoverArmed = true;
            } else if (hoverArmed) {
                openFlyout();
            }
            return;
        }
        if (overRow || overFlyout(mouseX, mouseY)) {
            leaveSince = -1;
        } else if (leaveSince < 0) {
            leaveSince = Util.getMillis();
        } else if (Util.getMillis() - leaveSince > GRACE_MS) {
            closeFlyout();
        }
    }

    private void renderFlyout(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        updateFlyout(mouseX, mouseY);
        float anim = flyAnim.value();
        if (!flyOpen && anim <= 0.01f) {
            return;
        }
        flyScroll = Math.max(0, Math.min(flyScroll, maxFlyScroll()));
        int fx = flyX(), fy = flyY(), fh = flyH();
        int dx = Math.round((1f - anim) * SLIDE_PX) * (flyLeft() ? 1 : -1);
        int ox = fx + dx;
        boolean live = flyOpen;
        RenderUtil.shadow(g, ox, fy, FLY_W, fh, 5);
        RenderUtil.roundedRectWithBorder(g, ox, fy, FLY_W, fh, 5,
                RenderUtil.withAlpha(Theme.POPOVER, anim), RenderUtil.withAlpha(Theme.POPOVER_BORDER, anim));
        RenderUtil.scissor(g, ox + 1, fy + 1, FLY_W - 2, fh - 2);
        FontFamily active = UiText.family();
        for (FlyItem item : FLY_ITEMS) {
            int iy = fy + item.y() - Math.round(flyScroll);
            if (iy + item.h() < fy || iy > fy + fh) {
                continue;
            }
            if (item.header() != null) {
                UiText.draw(g, item.header().title().toUpperCase(Locale.ROOT), ox + 10,
                        iy + (item.h() - UiText.lineHeight()) / 2 + 2,
                        RenderUtil.withAlpha(Theme.TEXT_FAINT, anim));
                continue;
            }
            FontFamily f = item.family();
            boolean isActive = f == active;
            if (live && RenderUtil.inside(mouseX, mouseY, fx + 2, iy, FLY_W - 4, item.h())
                    && RenderUtil.inside(mouseX, mouseY, fx, fy, FLY_W, fh)) {
                RenderUtil.roundedRect(g, ox + 2, iy, FLY_W - 4, item.h(), 3, Theme.ACCENT_SOFT);
            }
            int ty = iy + (item.h() - UiText.lineHeight()) / 2 + 1;
            UiText.drawAs(g, f.displayName(), f, ox + 10, ty,
                    RenderUtil.withAlpha(isActive ? Theme.ACCENT : Theme.TEXT, anim), UiText.Weight.REGULAR);
            if (isActive) {
                UiText.drawRight(g, "✓", ox + FLY_W - 9, ty, RenderUtil.withAlpha(Theme.ACCENT, anim));
            }
        }
        RenderUtil.unscissor(g);
    }

    /** Mini window drawn from the preset's own palette and style (independent of the active theme). */
    private static void drawPreview(GuiGraphicsExtractor g, ThemePreset preset, int x, int y) {
        ThemePreset.Palette p = preset.palette();
        ThemePreset.Style s = preset.style();
        // stand-in for the world behind translucent themes
        RenderUtil.roundedRect(g, x, y, PREVIEW_W, PREVIEW_H, 3, 0xFF2B2B38);
        RenderUtil.roundedRect(g, x, y, PREVIEW_W, PREVIEW_H, 3, p.panel());
        int sbW = 8;
        if (s.sidebarBlock()) {
            RenderUtil.roundedRect(g, x, y, sbW, PREVIEW_H, 3, p.sidebar(), true, false, true, false);
        }
        RenderUtil.rect(g, x + sbW, y + 1, 1, PREVIEW_H - 2, p.divider());
        // sidebar nav dot + content
        RenderUtil.rect(g, x + 2, y + 4, 4, 2, p.accent());
        RenderUtil.rect(g, x + 2, y + 8, 4, 2, p.textFaint());
        int cx = x + sbW + 3;
        int cw = PREVIEW_W - sbW - 6;
        if (s.cards()) {
            RenderUtil.roundedRect(g, cx, y + 3, cw, 5, 1, p.card());
            RenderUtil.roundedRect(g, cx, y + 10, cw, 5, 1, p.card());
            RenderUtil.rect(g, cx + 1, y + 5, 6, 1, p.text());
        } else {
            RenderUtil.rect(g, cx, y + 7, cw, 1, p.divider());
            RenderUtil.rect(g, cx, y + 14, cw, 1, p.divider());
            RenderUtil.rect(g, cx, y + 4, 6, 1, p.text());
            RenderUtil.rect(g, cx, y + 11, 6, 1, p.text());
        }
        // accent toggle dot
        RenderUtil.rect(g, x + PREVIEW_W - 6, y + (s.cards() ? 4 : 3), 3, 3, p.accent());
        RenderUtil.rect(g, x + PREVIEW_W - 6, y + (s.cards() ? 11 : 10), 3, 3, p.accentHover());
        RenderUtil.roundedRing(g, x, y, PREVIEW_W, PREVIEW_H, 3, p.border());
    }

    // ------------------------------------------------------------------ input

    /** Handles a click while open (inside the popover or flyout). Returns true when consumed. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!open || !overlayContains(mx, my)) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        if (overFlyout(mx, my)) {
            for (FlyItem item : FLY_ITEMS) {
                int iy = flyY() + item.y() - Math.round(flyScroll);
                if (item.family() != null && my >= iy && my < iy + item.h()) {
                    if (item.family() != UiText.family()) {
                        UiText.setFamily(item.family());
                    }
                    OptionWidget.playClick();
                    break;
                }
            }
            return true;
        }
        ThemePreset[] presets = ThemePreset.values();
        int row = (int) Math.floor((my - popY() - PAD) / ROW_H);
        if (row >= 0 && row < presets.length) {
            if (presets[row] != Theme.preset()) {
                Theme.apply(presets[row], true);
            }
            OptionWidget.playClick();
        } else if (overFontRow(mx, my)) {
            openFlyout(); // click also opens (accessibility); stays open
            OptionWidget.playClick();
        }
        return true;
    }

    /** Wheel over the flyout scrolls it. The event is always consumed while the popover is open. */
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (open && overFlyout(mx, my)) {
            flyScroll = Math.max(0, Math.min(flyScroll - (float) scrollY * 24f, maxFlyScroll()));
        }
        return open;
    }
}
