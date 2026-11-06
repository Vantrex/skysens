package de.vantrex.skysens.client.features.macro.editor.canvas;

import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

/** The zoom out / zoom in / fit / 100% buttons in the bottom left corner of the canvas. */
public final class CanvasControls {

    public enum Action {
        ZOOM_OUT("Zoom out"), ZOOM_IN("Zoom in"), FIT("Zoom to fit (Shift+1)"), RESET("Reset zoom to 100% (0)");

        private final String tooltip;

        Action(String tooltip) {
            this.tooltip = tooltip;
        }
    }

    private static final int BUTTON_W = 24;
    private static final int BUTTON_H = 22;
    private static final int MARGIN = 8;

    private int x;
    private int y;

    public void layout(int canvasX, int canvasY, int canvasW, int canvasH) {
        this.x = canvasX + MARGIN;
        this.y = canvasY + canvasH - MARGIN - BUTTON_H;
    }

    public int width() {
        return BUTTON_W * Action.values().length + 2;
    }

    public boolean contains(double mx, double my) {
        return RenderUtil.inside(mx, my, this.x, this.y, this.width(), BUTTON_H);
    }

    public @Nullable Action hit(double mx, double my) {
        if (!this.contains(mx, my)) {
            return null;
        }
        final int index = (int) ((mx - this.x - 1) / BUTTON_W);
        final Action[] actions = Action.values();
        return index >= 0 && index < actions.length ? actions[index] : null;
    }

    public void render(GuiGraphicsExtractor g, int mx, int my, double zoom, int canvasX, int canvasW) {
        RenderUtil.shadow(g, this.x, this.y, this.width(), BUTTON_H, 5);
        RenderUtil.roundedRectWithBorder(g, this.x, this.y, this.width(), BUTTON_H, 5, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        final Action hovered = this.hit(mx, my);
        final Action[] actions = Action.values();
        for (int i = 0; i < actions.length; i++) {
            final int bx = this.x + 1 + i * BUTTON_W;
            if (actions[i] == hovered) {
                RenderUtil.roundedRect(g, bx + 1, this.y + 1, BUTTON_W - 2, BUTTON_H - 2, 4, Theme.HOVER_OVERLAY);
            }
            final int color = actions[i] == hovered ? Theme.TEXT : Theme.TEXT_MUTED;
            final int cx = bx + BUTTON_W / 2;
            final int cy = this.y + BUTTON_H / 2;
            switch (actions[i]) {
                case ZOOM_OUT -> RenderUtil.rect(g, cx - 4, cy, 9, 1, color);
                case ZOOM_IN -> {
                    RenderUtil.rect(g, cx - 4, cy, 9, 1, color);
                    RenderUtil.rect(g, cx, cy - 4, 1, 9, color);
                }
                case FIT -> corners(g, cx, cy, color);
                case RESET -> UiText.drawCentered(g, "1:1", cx, cy - 4, color);
            }
        }
        UiText.draw(g, Math.round(zoom * 100) + "%", this.x + this.width() + 8, this.y + (BUTTON_H - 9) / 2, Theme.TEXT_FAINT);
        if (hovered != null) {
            final int tw = UiText.width(hovered.tooltip);
            final int tx = Math.max(canvasX + 4, Math.min(this.x, canvasX + canvasW - tw - 14));
            final int ty = this.y - 22;
            RenderUtil.roundedRectWithBorder(g, tx, ty, tw + 10, 16, 4, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
            UiText.draw(g, hovered.tooltip, tx + 5, ty + 4, Theme.TEXT);
        }
    }

    /** Four corner brackets ("fit to view"). */
    private static void corners(GuiGraphicsExtractor g, int cx, int cy, int color) {
        final int s = 5;
        final int l = 3;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                final int px = cx + sx * s;
                final int py = cy + sy * s;
                RenderUtil.rect(g, sx < 0 ? px : px - l + 1, py, l, 1, color);
                RenderUtil.rect(g, px, sy < 0 ? py : py - l + 1, 1, l, color);
            }
        }
    }

}
