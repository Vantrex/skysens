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
        // The curve is drawn as straight, rotated pieces (one fill each) instead of a fill per pixel: about 8 px
        // per piece is smooth at any zoom and keeps a wire at a few dozen draw calls.
        final int segments = (int) Math.max(8, Math.min(64, Math.ceil(controlLength / 8)));
        final int half = thickness / 2;
        double px = x1;
        double py = y1;
        for (int i = 1; i <= segments; i++) {
            final double t = (double) i / segments;
            final double u = 1 - t;
            final double b0 = u * u * u;
            final double b1 = 3 * u * u * t;
            final double b2 = 3 * u * t * t;
            final double b3 = t * t * t;
            final double nx = b0 * x1 + b1 * c1x + b2 * c2x + b3 * x2;
            final double ny = (b0 + b1) * y1 + (b2 + b3) * y2;
            final double dx = nx - px;
            final double dy = ny - py;
            final double length = Math.sqrt(dx * dx + dy * dy);
            if (length > 0.01) {
                g.pose().pushMatrix();
                g.pose().translate((float) px, (float) py);
                g.pose().rotate((float) Math.atan2(dy, dx));
                // overlap the neighbours by half the thickness, so the joints have no gaps
                g.fill(-half, -half, (int) Math.ceil(length) + half, thickness - half, argb);
                g.pose().popMatrix();
            }
            px = nx;
            py = ny;
        }
    }

    /** Point of the wire's bezier at t (0..1), in screen units. */
    public static double[] pointAt(double x1, double y1, double x2, double y2, double zoom, double t) {
        final double offset = Math.max(40, Math.abs(x2 - x1) / 2) * zoom;
        final double c1x = x1 + offset;
        final double c2x = x2 - offset;
        final double u = 1 - t;
        final double b0 = u * u * u;
        final double b1 = 3 * u * u * t;
        final double b2 = 3 * u * t * t;
        final double b3 = t * t * t;
        return new double[]{b0 * x1 + b1 * c1x + b2 * c2x + b3 * x2, (b0 + b1) * y1 + (b2 + b3) * y2};
    }

    /** Smallest distance from (px, py) to the wire, sampled. */
    public static double distance(double x1, double y1, double x2, double y2, double zoom, double px, double py) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= 32; i++) {
            final double[] p = pointAt(x1, y1, x2, y2, zoom, i / 32.0);
            best = Math.min(best, Math.hypot(p[0] - px, p[1] - py));
        }
        return best;
    }

}
