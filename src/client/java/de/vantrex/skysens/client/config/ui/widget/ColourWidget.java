package de.vantrex.skysens.client.config.ui.widget;

import de.vantrex.skysens.client.config.ui.model.OptionNode;
import de.vantrex.skysens.client.config.ui.render.Animated;
import de.vantrex.skysens.client.config.ui.render.RenderUtil;
import de.vantrex.skysens.client.config.ui.render.Theme;
import de.vantrex.skysens.client.config.ui.render.UiText;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

import java.awt.Color;
import java.util.Locale;

/**
 * Colour option: swatch + hex label, opening a popover with a saturation/brightness square, hue bar, alpha bar
 * and a chroma toggle. Edits write a new immutable {@link ChromaColour} live while dragging.
 */
public class ColourWidget extends CardWidget {

    private static final int SWATCH_W = 28;
    private static final int SWATCH_H = 14;
    private static final int GAP = 6;

    private static final int POP_W = 152;
    private static final int POP_H = 144;
    private static final int PAD = 8;
    private static final int CELL = 4;
    private static final int SQ = 108; // multiple of CELL
    private static final int BAR_W = 8;
    private static final int BAR_GAP = 6;
    private static final int ROW_H = 14;
    private static final int CHROMA_MS = 3000;

    private enum Drag { NONE, SQUARE, HUE, ALPHA }

    private boolean open;
    private Drag drag = Drag.NONE;
    private final Animated chromaKnob = new Animated(0f, 150f);

    public ColourWidget(OptionNode node) {
        super(node);
    }

    private ChromaColour current() {
        return node.get() instanceof ChromaColour c ? c : null;
    }

    @Override
    protected int controlWidth() {
        return SWATCH_W + GAP + hexLabelWidth();
    }

    /**
     * Widest possible "#RRGGBB" label. With a proportional font (Inter) e.g. "#000000" is wider than "#FFFFFF",
     * so reserve the widest hex digit for each of the six positions (glyph advances add up, no kerning).
     */
    private static int hexLabelWidth() {
        int widest = 0;
        for (char c : "0123456789ABCDEF".toCharArray()) {
            widest = Math.max(widest, UiText.width(String.valueOf(c)));
        }
        return UiText.width("#") + 6 * widest;
    }

    @Override
    protected int controlHeight() {
        return SWATCH_H;
    }

    // ------------------------------------------------------------------ geometry

    private boolean overControl(double mx, double my) {
        return RenderUtil.inside(mx, my, controlX(), controlY(), controlWidth(), SWATCH_H);
    }

    /** Popover origin {x, y}, flipped/clamped to stay on screen. */
    private int[] popOrigin() {
        var window = Minecraft.getInstance().getWindow();
        int sw = window.getGuiScaledWidth();
        int sh = window.getGuiScaledHeight();
        int px = Math.max(4, Math.min(controlX() + controlWidth() - POP_W, sw - POP_W - 4));
        int py = controlY() + SWATCH_H + 2;
        if (py + POP_H > sh - 4) {
            int above = controlY() - 2 - POP_H;
            py = above >= 4 ? above : Math.max(4, sh - 4 - POP_H);
        }
        return new int[]{px, py};
    }

    private boolean inPopover(double mx, double my) {
        int[] o = popOrigin();
        return RenderUtil.inside(mx, my, o[0], o[1], POP_W, POP_H);
    }

    private static int hsb(float h, float s, float b) {
        return Color.HSBtoRGB(h, s, b);
    }

    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ control rendering

    @Override
    protected void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY,
                                 float partialTick) {
        ChromaColour c = current();
        int rgb = c == null ? 0 : c.getEffectiveColourRGB() & 0xFFFFFF;
        int alpha = c == null ? 255 : c.getAlpha();
        boolean hover = overControl(mouseX, mouseY);
        int border = open ? Theme.ACCENT : hover ? Theme.TEXT_FAINT : Theme.BORDER;
        swatch(g, cx, cy, SWATCH_W, SWATCH_H, (alpha << 24) | rgb, border);
        UiText.draw(g, hex(rgb), cx + SWATCH_W + GAP, cy + (SWATCH_H - UiText.lineHeight()) / 2 + 1,
                Theme.TEXT_MUTED);
    }

    private void swatch(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb, int border) {
        RenderUtil.rect(g, x, y, w, h, border);
        RenderUtil.checkerboard(g, x + 1, y + 1, w - 2, h - 2, 4);
        if ((argb >>> 24) > 0) {
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, argb);
        }
    }

    // ------------------------------------------------------------------ popover rendering

    @Override
    public void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }
        ChromaColour c = current();
        if (c == null) {
            open = false;
            return;
        }
        int[] o = popOrigin();
        int px = o[0], py = o[1];
        float h = c.getHue(), s = c.getSaturation(), b = c.getBrightness();
        int alpha = c.getAlpha();
        int baseRgb = hsb(h, s, b) & 0xFFFFFF;

        RenderUtil.shadow(g, px, py, POP_W, POP_H, 5);
        RenderUtil.roundedRectWithBorder(g, px, py, POP_W, POP_H, 5, Theme.POPOVER, Theme.POPOVER_BORDER);

        int sqX = px + PAD;
        int sqY = py + PAD;
        int hueX = sqX + SQ + BAR_GAP;
        int alphaX = hueX + BAR_W + BAR_GAP;

        // saturation / brightness square
        int cells = SQ / CELL;
        for (int cyI = 0; cyI < cells; cyI++) {
            float bb = 1f - (cyI + 0.5f) / cells;
            for (int cxI = 0; cxI < cells; cxI++) {
                float ss = (cxI + 0.5f) / cells;
                int x0 = sqX + cxI * CELL;
                int y0 = sqY + cyI * CELL;
                g.fill(x0, y0, x0 + CELL, y0 + CELL, hsb(h, ss, bb));
            }
        }
        int mx = sqX + Math.round(s * (SQ - 1));
        int my = sqY + Math.round((1f - b) * (SQ - 1));
        RenderUtil.circle(g, mx, my, 4, 0xFF000000);
        RenderUtil.circle(g, mx, my, 3, Theme.KNOB);
        RenderUtil.circle(g, mx, my, 2, 0xFF000000 | baseRgb);

        // hue bar
        for (int row = 0; row < SQ; row++) {
            g.fill(hueX, sqY + row, hueX + BAR_W, sqY + row + 1, hsb(row / (float) (SQ - 1), 1f, 1f));
        }
        barMarker(g, hueX, sqY + Math.round(h * (SQ - 1)));

        // alpha bar
        RenderUtil.checkerboard(g, alphaX, sqY, BAR_W, SQ, 4);
        for (int row = 0; row < SQ; row++) {
            int a = Math.round(255f * (1f - row / (float) (SQ - 1)));
            if (a > 0) {
                g.fill(alphaX, sqY + row, alphaX + BAR_W, sqY + row + 1, (a << 24) | baseRgb);
            }
        }
        barMarker(g, alphaX, sqY + Math.round((1f - alpha / 255f) * (SQ - 1)));

        // bottom row: chroma toggle + hex preview
        int rowY = sqY + SQ + BAR_GAP;
        boolean chroma = c.getTimeForFullRotationInMillis() > 0;
        chromaKnob.target(chroma ? 1f : 0f);
        float t = chromaKnob.value();
        int tx = px + PAD;
        int ty = rowY + (ROW_H - 12) / 2;
        RenderUtil.roundedRect(g, tx, ty, 22, 12, 6, RenderUtil.lerpColor(t, Theme.TRACK, Theme.ACCENT));
        RenderUtil.roundedRect(g, tx + 2 + Math.round(t * 10), ty + 2, 8, 8, 4, Theme.KNOB);
        UiText.draw(g, "Chroma", tx + 22 + 4, rowY + (ROW_H - UiText.lineHeight()) / 2 + 1,
                chroma ? Theme.TEXT : Theme.TEXT_MUTED);
        UiText.drawRight(g, hex(baseRgb), px + POP_W - PAD, rowY + (ROW_H - UiText.lineHeight()) / 2 + 1,
                Theme.TEXT_MUTED);
    }

    private void barMarker(GuiGraphicsExtractor g, int barX, int y) {
        g.fill(barX - 2, y - 2, barX + BAR_W + 2, y + 3, 0xFF000000);
        g.fill(barX - 1, y - 1, barX + BAR_W + 1, y + 2, Theme.KNOB);
    }

    // ------------------------------------------------------------------ overlay contract

    @Override
    public boolean hasOverlay() {
        return open;
    }

    @Override
    public boolean overlayContains(double mx, double my) {
        return open && inPopover(mx, my);
    }

    @Override
    public void closeOverlay() {
        open = false;
        drag = Drag.NONE;
    }

    // ------------------------------------------------------------------ input

    private int sqX() {
        return popOrigin()[0] + PAD;
    }

    private int sqY() {
        return popOrigin()[1] + PAD;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (open) {
            if (inPopover(mx, my)) {
                if (button == 0) {
                    handlePopoverClick(mx, my);
                }
                return true;
            }
            closeOverlay();
            // clicking the swatch while open just closes the popover
            return overControl(mx, my);
        }
        if (button == 0 && overControl(mx, my) && current() != null) {
            open = true;
            drag = Drag.NONE;
            playClick();
            return true;
        }
        return false;
    }

    private void handlePopoverClick(double mx, double my) {
        int sqX = sqX();
        int sqY = sqY();
        int hueX = sqX + SQ + BAR_GAP;
        int alphaX = hueX + BAR_W + BAR_GAP;
        if (RenderUtil.inside(mx, my, sqX, sqY, SQ, SQ)) {
            drag = Drag.SQUARE;
            applyDrag(mx, my);
        } else if (RenderUtil.inside(mx, my, hueX - 2, sqY, BAR_W + 4, SQ)) {
            drag = Drag.HUE;
            applyDrag(mx, my);
        } else if (RenderUtil.inside(mx, my, alphaX - 2, sqY, BAR_W + 4, SQ)) {
            drag = Drag.ALPHA;
            applyDrag(mx, my);
        } else {
            int rowY = sqY + SQ + BAR_GAP;
            int tx = sqX;
            int w = 22 + 4 + UiText.width("Chroma");
            if (RenderUtil.inside(mx, my, tx, rowY, w, ROW_H)) {
                ChromaColour c = current();
                if (c != null) {
                    int ms = c.getTimeForFullRotationInMillis() > 0 ? 0 : CHROMA_MS;
                    node.set(new ChromaColour(c.getHue(), c.getSaturation(), c.getBrightness(), ms,
                            c.getAlpha()));
                    playClick();
                }
            }
        }
    }

    private void applyDrag(double mx, double my) {
        ChromaColour c = current();
        if (c == null || drag == Drag.NONE) {
            return;
        }
        float h = c.getHue(), s = c.getSaturation(), b = c.getBrightness();
        int a = c.getAlpha();
        int sqX = sqX();
        int sqY = sqY();
        switch (drag) {
            case SQUARE -> {
                s = clamp01((mx - sqX) / (SQ - 1));
                b = 1f - clamp01((my - sqY) / (SQ - 1));
            }
            case HUE -> h = clamp01((my - sqY) / (SQ - 1));
            case ALPHA -> a = Math.round(255f * (1f - clamp01((my - sqY) / (SQ - 1))));
            default -> {
                return;
            }
        }
        ChromaColour n = new ChromaColour(h, s, b, c.getTimeForFullRotationInMillis(), a);
        if (!n.equals(c)) {
            node.set(n);
        }
    }

    private static float clamp01(double v) {
        return (float) Math.max(0.0, Math.min(1.0, v));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (open && drag != Drag.NONE && button == 0) {
            applyDrag(mx, my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drag != Drag.NONE && button == 0) {
            drag = Drag.NONE;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        return open && inPopover(mx, my);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (open && e.isEscape()) {
            closeOverlay();
            return true;
        }
        return false;
    }
}
