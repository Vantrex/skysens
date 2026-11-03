package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/**
 * Shared pieces of the destructive-action confirmations (the per-option reset popover and the reset-all dialog):
 * a Cancel button plus a danger button that stays disabled behind a visible countdown for a few seconds.
 * Bounds are plain {@code {x, y, w, h}} arrays.
 */
public final class ConfirmButtons {

    private ConfirmButtons() {
    }

    /** Milliseconds left until a confirmation opened at {@code openedAt} (see {@link Util#getMillis}) is ready. */
    public static long remainingMs(long openedAt, long delayMs) {
        return Math.max(0, delayMs - (Util.getMillis() - openedAt));
    }

    /** The danger button's label: {@code "Label (3)"} while counting down, plain {@code "Label"} once ready. */
    public static String label(String label, long remainingMs) {
        return remainingMs > 0 ? label + " (" + (remainingMs + 999) / 1000 + ")" : label;
    }

    /** Bounds of the button at {@code index} (0 = Cancel, 1 = confirm) in a row of two along the bottom of a panel. */
    public static int[] button(int[] panel, int index, int pad, int height, int gap) {
        int bw = (panel[2] - 2 * pad - gap) / 2;
        int by = panel[1] + panel[3] - pad - height;
        return new int[]{panel[0] + pad + index * (bw + gap), by, bw, height};
    }

    /** Draws a rounded button with a centred semibold label, brightened while the mouse is over it. */
    public static void draw(GuiGraphicsExtractor g, int[] b, String label, int fill, int fillHover, int textColour,
                            int mouseX, int mouseY) {
        boolean hover = RenderUtil.inside(mouseX, mouseY, b[0], b[1], b[2], b[3]);
        RenderUtil.roundedRect(g, b[0], b[1], b[2], b[3], 4, hover ? fillHover : fill);
        UiText.drawCentered(g, label, b[0] + b[2] / 2, b[1] + (b[3] - UiText.lineHeight()) / 2 + 1, textColour,
                UiText.Weight.SEMIBOLD);
    }

    /** Draws the neutral Cancel button. */
    public static void drawCancel(GuiGraphicsExtractor g, int[] b, int mouseX, int mouseY) {
        draw(g, b, "Cancel", Theme.TRACK, Theme.CARD_HOVER, Theme.TEXT, mouseX, mouseY);
    }

    /** Draws the danger button; dimmed and hover-less until {@code remainingMs} has run out. */
    public static void drawDanger(GuiGraphicsExtractor g, int[] b, String label, long remainingMs, int mouseX,
                                  int mouseY) {
        boolean ready = remainingMs == 0;
        float a = ready ? 1f : 0.4f;
        draw(g, b, label(label, remainingMs), RenderUtil.withAlpha(Theme.DANGER, ready ? 0.85f : a * 0.6f),
                RenderUtil.withAlpha(Theme.DANGER, ready ? 1f : a * 0.6f), RenderUtil.withAlpha(0xFFFFFFFF, a),
                ready ? mouseX : -1, mouseY);
    }
}
