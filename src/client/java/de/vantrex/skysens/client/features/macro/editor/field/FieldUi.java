package de.vantrex.skysens.client.features.macro.editor.field;

import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing helpers shared by the parameter fields. */
public final class FieldUi {

    public static final int TOGGLE_W = 22;
    public static final int TOGGLE_H = 12;
    public static final int ICON = 16;
    public static final int AMBER = 0xFFE3A52B;

    public enum Icon { UP, DOWN, CLOSE }

    private FieldUi() {
    }

    public static void toggle(GuiGraphicsExtractor g, int x, int y, boolean on) {
        RenderUtil.roundedRect(g, x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, on ? Theme.ACCENT : Theme.TRACK);
        final int r = (TOGGLE_H - 4) / 2;
        RenderUtil.knob(g, on ? x + TOGGLE_W - 2 - r : x + 2 + r, y + TOGGLE_H / 2, r, on ? 1f : 0f);
    }

    /** A small square button with a drawn glyph. */
    public static void iconButton(GuiGraphicsExtractor g, int x, int y, Icon icon, boolean enabled, int mx, int my) {
        final boolean hover = enabled && RenderUtil.inside(mx, my, x, y, ICON, ICON);
        RenderUtil.roundedRect(g, x, y, ICON, ICON, 3, hover ? Theme.CARD_HOVER : Theme.CARD);
        final int color = !enabled ? Theme.TEXT_FAINT : icon == Icon.CLOSE && hover ? Theme.DANGER : Theme.TEXT_MUTED;
        final int cx = x + ICON / 2;
        final int cy = y + ICON / 2;
        switch (icon) {
            case UP -> {
                for (int i = 0; i < 4; i++) {
                    RenderUtil.rect(g, cx - i, cy - 2 + i, 2 * i + 1, 1, color);
                }
            }
            case DOWN -> {
                for (int i = 0; i < 4; i++) {
                    RenderUtil.rect(g, cx - i, cy + 1 - i, 2 * i + 1, 1, color);
                }
            }
            case CLOSE -> {
                for (int i = -3; i <= 3; i++) {
                    RenderUtil.rect(g, cx + i, cy + i, 1, 1, color);
                    RenderUtil.rect(g, cx + i, cy - i, 1, 1, color);
                }
            }
        }
    }

    public static boolean inIcon(double mx, double my, int x, int y) {
        return RenderUtil.inside(mx, my, x, y, ICON, ICON);
    }

    /** A text button; returns nothing, the caller tests the same rectangle on click. */
    public static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int mx, int my) {
        final boolean hover = RenderUtil.inside(mx, my, x, y, w, h);
        RenderUtil.roundedRect(g, x, y, w, h, 4, hover ? Theme.CARD_HOVER : Theme.CARD);
        UiText.drawCentered(g, label, x + w / 2, y + (h - 9) / 2 + 1, hover ? Theme.TEXT : Theme.TEXT_MUTED);
    }

    public static void label(GuiGraphicsExtractor g, String text, int x, int y) {
        UiText.draw(g, text, x, y, Theme.TEXT_MUTED);
    }

}
