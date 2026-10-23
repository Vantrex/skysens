package de.vantrex.skysens.client.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

/**
 * Drawing helpers on top of {@link GuiGraphicsExtractor}. All colours are ARGB ints with non-zero alpha.
 * All rectangle helpers take {@code x, y, width, height} unless stated otherwise.
 */
public final class RenderUtil {

    private RenderUtil() {
    }

    // ------------------------------------------------------------------ rectangles

    /** Filled rectangle given as x, y, width, height. */
    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.fill(x, y, x + w, y + h, argb);
    }

    /** Filled rectangle with all four corners rounded by radius {@code r}. */
    public static void roundedRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int argb) {
        roundedRect(g, x, y, w, h, r, argb, true, true, true, true);
    }

    /**
     * Filled rounded rectangle with individually selectable corners (tl = top-left, tr = top-right,
     * bl = bottom-left, br = bottom-right). Drawn as per-scanline fills.
     */
    public static void roundedRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int argb,
                                   boolean tl, boolean tr, boolean bl, boolean br) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.min(r, Math.min(w / 2, h / 2));
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, argb);
            return;
        }
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            // top row i
            int l = tl ? inset : 0;
            int rr = tr ? inset : 0;
            g.fill(x + l, y + i, x + w - rr, y + i + 1, argb);
            // bottom row i (mirrored)
            l = bl ? inset : 0;
            rr = br ? inset : 0;
            g.fill(x + l, y + h - 1 - i, x + w - rr, y + h - i, argb);
        }
        if (h - 2 * r > 0) {
            g.fill(x, y + r, x + w, y + h - r, argb);
        }
    }

    private static int cornerInset(int r, int row) {
        double dy = r - row - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, (double) r * r - dy * dy)));
    }

    /**
     * Rounded rectangle with a 1px rounded border: border colour outside, fill colour inside. For translucent
     * colours the border is drawn as a true ring so the fill never blends with the border colour underneath.
     */
    public static void roundedRectWithBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int r,
                                             int fill, int border) {
        if ((fill >>> 24) == 0xFF && (border >>> 24) == 0xFF) {
            roundedRect(g, x, y, w, h, r, border);
            roundedRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
            return;
        }
        roundedRing(g, x, y, w, h, r, border);
        roundedRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    /** Horizontal inset of scanline {@code row} of a rounded rectangle of height {@code h} and radius {@code r}. */
    private static int rowInset(int r, int h, int row) {
        r = Math.min(r, h / 2);
        if (r <= 0) {
            return 0;
        }
        if (row < r) {
            return cornerInset(r, row);
        }
        if (row >= h - r) {
            return cornerInset(r, h - 1 - row);
        }
        return 0;
    }

    /** Hollow 1px rounded ring (the outline of {@link #roundedRect}); the inside is left untouched. */
    public static void roundedRing(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int argb) {
        if (w <= 2 || h <= 2) {
            roundedRect(g, x, y, w, h, r, argb);
            return;
        }
        // same clamp as roundedRect, so the ring's hole matches the inner fill of roundedRectWithBorder
        r = Math.max(0, Math.min(r, Math.min(w / 2, h / 2)));
        int ir = Math.max(0, r - 1);
        for (int row = 0; row < h; row++) {
            int oi = rowInset(r, h, row);
            if (row == 0 || row == h - 1) {
                g.fill(x + oi, y + row, x + w - oi, y + row + 1, argb);
                continue;
            }
            int ii = rowInset(ir, h - 2, row - 1);
            int leftEnd = Math.max(x + oi + 1, x + 1 + ii);
            int rightStart = Math.min(x + w - oi - 1, x + w - 1 - ii);
            g.fill(x + oi, y + row, leftEnd, y + row + 1, argb);
            g.fill(rightStart, y + row, x + w - oi, y + row + 1, argb);
        }
    }

    /**
     * Rounded rectangle that is a bordered input box when the theme draws input borders or {@code active},
     * and a plain filled box otherwise.
     */
    public static void inputBox(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, boolean active,
                                int activeBorder, int idleBorder) {
        if (Theme.style().inputBorders() || active) {
            roundedRectWithBorder(g, x, y, w, h, r, Theme.INPUT, active ? activeBorder : idleBorder);
        } else {
            roundedRect(g, x, y, w, h, r, Theme.INPUT);
        }
    }

    /** Filled circle centred on cx, cy with radius {@code r}. */
    public static void circle(GuiGraphicsExtractor g, int cx, int cy, int r, int argb) {
        roundedRect(g, cx - r, cy - r, r * 2, r * 2, r, argb);
    }

    /**
     * Knob / thumb: a white circle with the theme's subtle ring (light themes) and accent glow halo (glass).
     *
     * @param glowAmount 0..1 strength of the halo, only used when the active style has glow
     */
    public static void knob(GuiGraphicsExtractor g, int cx, int cy, int r, float glowAmount) {
        if (Theme.style().glow() && glowAmount > 0.01f) {
            circle(g, cx, cy, r + 3, withAlpha(Theme.ACCENT_SOFT, 0.5f * glowAmount));
            circle(g, cx, cy, r + 2, withAlpha(Theme.ACCENT_SOFT, glowAmount));
        }
        if (Theme.style().knobRing()) {
            circle(g, cx, cy, r + 1, Theme.KNOB_RING);
        }
        circle(g, cx, cy, r, Theme.KNOB);
    }

    /** Alphas of the shadow rings, outermost first. */
    private static final int[] SHADOW_ALPHAS = {0x10, 0x2E, 0x55};

    /**
     * Soft drop shadow around a rounded rectangle: three hollow 1px rings outside the rectangle (so translucent
     * panels are not darkened from underneath), strongest nearest to the edge. Draw BEFORE the rectangle.
     */
    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r) {
        for (int i = 0; i < SHADOW_ALPHAS.length; i++) {
            int o = SHADOW_ALPHAS.length - i;
            roundedRing(g, x - o, y - o, w + o * 2, h + o * 2, r + o, ARGB.color(SHADOW_ALPHAS[i], 0, 0, 0));
        }
    }

    /** Light/dark checkerboard (for colour alpha previews) clipped exactly to x, y, w, h. */
    public static void checkerboard(GuiGraphicsExtractor g, int x, int y, int w, int h, int cell) {
        if (cell <= 0) {
            return;
        }
        for (int cy = 0; cy < h; cy += cell) {
            for (int cx = 0; cx < w; cx += cell) {
                boolean odd = ((cx / cell) + (cy / cell) & 1) == 1;
                int x2 = Math.min(cx + cell, w);
                int y2 = Math.min(cy + cell, h);
                g.fill(x + cx, y + cy, x + x2, y + y2, odd ? 0xFF5E5E5E : 0xFF8C8C8C);
            }
        }
    }

    // ------------------------------------------------------------------ scissor

    /**
     * Clips drawing to the rectangle at x, y with the given width and height (screen/GUI coordinates).
     * <p>
     * Verified by bytecode: {@code GuiGraphicsExtractor.enableScissor(int x1, int y1, int x2, int y2)} takes
     * <b>corner coordinates</b> (min x, min y, max x, max y), internally building
     * {@code ScreenRectangle(x1, y1, x2 - x1, y2 - y1)} and transforming it by the current pose. This wrapper
     * always takes x, y, width, height and converts. Scissors nest (intersect); every call must be paired with
     * {@link #unscissor(GuiGraphicsExtractor)}.
     */
    public static void scissor(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + Math.max(0, w), y + Math.max(0, h));
    }

    /** Pops the scissor pushed by {@link #scissor}. */
    public static void unscissor(GuiGraphicsExtractor g) {
        g.disableScissor();
    }

    // ------------------------------------------------------------------ colours

    /** Interpolates between two ARGB colours (sRGB), t in 0..1. */
    public static int lerpColor(float t, int a, int b) {
        return ARGB.srgbLerp(Math.max(0f, Math.min(1f, t)), a, b);
    }

    /** Multiplies the alpha channel of a colour by {@code factor} (0..1). */
    public static int withAlpha(int argb, float factor) {
        return ARGB.multiplyAlpha(argb, Math.max(0f, Math.min(1f, factor)));
    }

    /** Whether the point is inside [x, x+w) x [y, y+h). */
    public static boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
