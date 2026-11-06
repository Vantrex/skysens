package de.vantrex.skysens.client.features.macro.editor.canvas;

import de.vantrex.skysens.client.features.macro.model.ViewportState;

/**
 * Pan and zoom of a canvas. {@code panX/panY} are in screen units relative to the canvas' top left corner:
 * {@code screen = world * zoom + pan}. Zoom uses discrete steps, because glyphs at arbitrary fractional scales look soft.
 */
public final class Viewport {

    public static final double[] STEPS = {0.5, 0.67, 0.8, 1.0, 1.25, 1.5, 2.0};

    public double panX;
    public double panY;
    public double zoom = 1.0;

    public void load(ViewportState state) {
        this.panX = state.x;
        this.panY = state.y;
        this.zoom = snap(state.zoom);
    }

    public void store(ViewportState state) {
        state.x = this.panX;
        state.y = this.panY;
        state.zoom = this.zoom;
    }

    /** Whether the saved state is the untouched default, i.e. the workflow was never panned or zoomed. */
    public static boolean isDefault(ViewportState state) {
        return state.x == 0 && state.y == 0 && state.zoom == 1.0;
    }

    /** Whole pixels, so grid, wires and nodes (drawn under a pose translation) agree on the same offset. */
    public int originX() {
        return (int) Math.round(this.panX);
    }

    public int originY() {
        return (int) Math.round(this.panY);
    }

    public double toScreenX(double worldX) {
        return worldX * this.zoom + this.originX();
    }

    public double toScreenY(double worldY) {
        return worldY * this.zoom + this.originY();
    }

    public double toWorldX(double screenX) {
        return (screenX - this.originX()) / this.zoom;
    }

    public double toWorldY(double screenY) {
        return (screenY - this.originY()) / this.zoom;
    }

    public void pan(double dx, double dy) {
        this.panX += dx;
        this.panY += dy;
    }

    /** One zoom step in (+1) or out (-1), keeping the world point under the screen point (cx, cy) fixed. */
    public void zoomStep(double cx, double cy, int direction) {
        int index = 0;
        for (int i = 0; i < STEPS.length; i++) {
            if (Math.abs(STEPS[i] - this.zoom) < Math.abs(STEPS[index] - this.zoom)) {
                index = i;
            }
        }
        final int next = Math.max(0, Math.min(STEPS.length - 1, index + direction));
        this.zoomTo(cx, cy, STEPS[next]);
    }

    public void zoomTo(double cx, double cy, double newZoom) {
        final double worldX = this.toWorldX(cx);
        final double worldY = this.toWorldY(cy);
        this.zoom = newZoom;
        this.panX = cx - worldX * newZoom;
        this.panY = cy - worldY * newZoom;
    }

    /** Fits the world bounds into a view of the given size (both relative to the canvas), never zooming in past 100%. */
    public void fit(double minX, double minY, double maxX, double maxY, int viewW, int viewH, int padding) {
        final double boundsW = Math.max(1, maxX - minX);
        final double boundsH = Math.max(1, maxY - minY);
        final double wanted = Math.min((viewW - 2.0 * padding) / boundsW, (viewH - 2.0 * padding) / boundsH);
        double chosen = STEPS[0];
        for (double step : STEPS) {
            if (step <= wanted && step <= 1.0) {
                chosen = step;
            }
        }
        this.zoom = chosen;
        this.panX = viewW / 2.0 - (minX + maxX) / 2.0 * chosen;
        this.panY = viewH / 2.0 - (minY + maxY) / 2.0 * chosen;
    }

    private static double snap(double value) {
        double best = STEPS[0];
        for (double step : STEPS) {
            if (Math.abs(step - value) < Math.abs(best - value)) {
                best = step;
            }
        }
        return best;
    }

}
