package de.vantrex.skysens.client.features.macro.editor.canvas;

import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The canvas background: a dark surface with a dot every 20 world units. */
public final class GridRenderer {

    private static final int SPACING = 20;

    private GridRenderer() {
    }

    /** The panel colour pushed towards black, so the canvas reads as a darker work surface on every theme. */
    public static int background() {
        final int panel = Theme.PANEL | 0xFF000000;
        final float luminance = (((panel >> 16) & 0xFF) * 0.299f + ((panel >> 8) & 0xFF) * 0.587f + (panel & 0xFF) * 0.114f) / 255f;
        return RenderUtil.lerpColor(luminance < 0.5f ? 0.4f : 0.08f, panel, 0xFF000000);
    }

    public static void render(GuiGraphicsExtractor g, int x, int y, int w, int h, Viewport viewport) {
        RenderUtil.rect(g, x, y, w, h, background());
        // at low zoom only every other dot, and fainter
        final int stride = viewport.zoom < 0.6 ? 2 : 1;
        final double step = SPACING * stride * viewport.zoom;
        final int dot = viewport.zoom >= 1.25 ? 2 : 1;
        final float fade = viewport.zoom < 0.8 ? 0.45f : 0.7f;
        final int color = RenderUtil.withAlpha(Theme.TEXT_FAINT, fade);
        final double startX = x + mod(viewport.originX(), step);
        final double startY = y + mod(viewport.originY(), step);
        for (double dy = startY; dy < y + h; dy += step) {
            final int py = (int) Math.round(dy);
            for (double dx = startX; dx < x + w; dx += step) {
                final int px = (int) Math.round(dx);
                g.fill(px, py, px + dot, py + dot, color);
            }
        }
    }

    private static double mod(double value, double step) {
        final double m = value % step;
        return m < 0 ? m + step : m;
    }

}
