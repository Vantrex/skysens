package de.vantrex.skysens.client.features.macro.editor.canvas;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws a wire as a smooth cubic bezier in screen space. The curve is stamped as small squares spaced at most one
 * thickness apart, so there are no gaps and no rotated quads are needed (only axis-aligned fills exist).
 */
public final class WireRenderer {

    private WireRenderer() {
    }

    /** Thickness of a normal wire at the given zoom. */
    public static int thickness(double zoom) {
        return Math.max(1, (int) Math.round(2 * zoom));
    }

    /** Whether the wire's bounding box touches the clip rectangle (cheap culling). */
    public static boolean visible(double x1, double y1, double x2, double y2, double zoom,
                                  int clipX, int clipY, int clipW, int clipH) {
        final double reach = Math.max(40, Math.abs(x2 - x1) / 2) * zoom;
        final double minX = Math.min(x1, x2) - reach;
        final double maxX = Math.max(x1, x2) + reach;
        final double minY = Math.min(y1, y2) - 4;
        final double maxY = Math.max(y1, y2) + 4;
        return maxX >= clipX && minX <= clipX + clipW && maxY >= clipY && minY <= clipY + clipH;
    }

    /** From the output port at (x1, y1) to the input port at (x2, y2), both in screen units. */
    public static void draw(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, double zoom,
                            int thickness, int argb) {
        final double offset = Math.max(40, Math.abs(x2 - x1) / 2) * zoom;
        final double c1x = x1 + offset;
        final double c2x = x2 - offset;
        final double controlLength = Math.abs(c1x - x1) + Math.hypot(c2x - c1x, y2 - y1) + Math.abs(x2 - c2x);
        final int samples = (int) Math.min(4000, Math.ceil(controlLength / thickness) + 1);
        final int half = thickness / 2;
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        for (int i = 0; i <= samples; i++) {
            final double t = (double) i / samples;
            final double u = 1 - t;
            final double b0 = u * u * u;
            final double b1 = 3 * u * u * t;
            final double b2 = 3 * u * t * t;
            final double b3 = t * t * t;
            final int px = (int) Math.round(b0 * x1 + b1 * c1x + b2 * c2x + b3 * x2) - half;
            final int py = (int) Math.round((b0 + b1) * y1 + (b2 + b3) * y2) - half;
            if (px == lastX && py == lastY) {
                continue;
            }
            lastX = px;
            lastY = py;
            g.fill(px, py, px + thickness, py + thickness, argb);
        }
    }

}
