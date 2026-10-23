package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.Animated;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Numeric option rendered as a pill slider with a value chip. Writes the exact boxed type of the option.
 */
public class SliderWidget extends CardWidget {

    private static final int TRACK_W = 100;
    private static final int CHIP_W = 30;
    private static final int GAP = 6;
    private static final int H = 14;

    private final double min;
    private final double max;
    private final double step;
    private final Animated thumb = new Animated(8f, 100f);
    private int lastThumbBase = -1;
    private boolean dragging;
    /** Left x of the track as of the last render/layout; used by drag handling. */
    private int trackX;

    public SliderWidget(OptionNode node) {
        super(node);
        ConfigEditorSlider s = node.annotation(ConfigEditorSlider.class);
        if (s != null) {
            this.min = s.minValue();
            this.max = Math.max(s.minValue(), s.maxValue());
            this.step = s.minStep();
        } else {
            this.min = 0;
            this.max = 100;
            this.step = 1;
        }
    }

    @Override
    protected int controlWidth() {
        return TRACK_W + GAP + CHIP_W;
    }

    @Override
    protected int controlHeight() {
        return H;
    }

    private boolean isIntegral() {
        Class<?> t = node.valueType();
        return t == int.class || t == Integer.class || t == long.class || t == Long.class;
    }

    private double current() {
        return node.get() instanceof Number n ? n.doubleValue() : min;
    }

    private double snap(double v) {
        double r = v;
        if (step > 0) {
            r = min + Math.round((v - min) / step) * step;
        }
        return Math.max(min, Math.min(max, r));
    }

    private Object box(double v) {
        Class<?> t = node.valueType();
        if (t == int.class || t == Integer.class) {
            return Integer.valueOf((int) Math.round(v));
        }
        if (t == long.class || t == Long.class) {
            return Long.valueOf(Math.round(v));
        }
        if (t == double.class || t == Double.class) {
            return Double.valueOf(Math.round(v * 1_000_000d) / 1_000_000d);
        }
        return Float.valueOf((float) v);
    }

    private void setValue(double v) {
        Object nv = box(snap(v));
        if (!nv.equals(node.get())) {
            node.set(nv);
        }
    }

    private void setFromMouse(double mx) {
        double frac = Math.max(0.0, Math.min(1.0, (mx - trackX) / TRACK_W));
        setValue(min + frac * (max - min));
    }

    private String format(double v) {
        if (isIntegral() || step >= 1) {
            return String.valueOf(Math.round(v));
        }
        return String.format(Locale.ROOT, step < 0.1 ? "%.2f" : "%.1f", v);
    }

    private boolean overSlider(double mx, double my) {
        return mx >= controlX() - 4 && mx < controlX() + TRACK_W + 4
                && my >= controlY() && my < controlY() + H;
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY,
                                 float partialTick) {
        trackX = cx;
        double cur = Math.max(min, Math.min(max, current()));
        double frac = max > min ? (cur - min) / (max - min) : 0.0;
        int midY = cy + H / 2;
        int thumbCx = cx + (int) Math.round(frac * TRACK_W);

        int th = Theme.style().sliderTrackH();
        int trackTop = midY - th / 2;
        RenderUtil.roundedRect(g, cx, trackTop, TRACK_W, th, th / 2, Theme.TRACK);
        int fillW = thumbCx - cx;
        if (fillW > 0) {
            RenderUtil.roundedRect(g, cx, trackTop, fillW, th, th / 2, Theme.ACCENT);
        }

        int base = Theme.style().thumbSize();
        if (base != lastThumbBase) {
            lastThumbBase = base;
            thumb.snap(base);
        }
        boolean active = dragging || overSlider(mouseX, mouseY);
        thumb.target(active ? base + 2 : base);
        int r = Math.round(thumb.value() / 2f);
        RenderUtil.knob(g, thumbCx, midY, r, active ? 1f : 0.6f);

        int chipX = cx + TRACK_W + GAP;
        RenderUtil.inputBox(g, chipX, cy, CHIP_W, H, 3, false, Theme.ACCENT, Theme.BORDER);
        String label = format(cur);
        UiText.drawCentered(g, label, chipX + CHIP_W / 2, cy + (H - UiText.lineHeight()) / 2 + 1,
                Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overSlider(mx, my)) {
            trackX = controlX();
            dragging = true;
            setFromMouse(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            setFromMouse(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }

    // No mouseScrolled override: the wheel always scrolls the page, so scrolling past a
    // slider (e.g. mouse sensitivity) can never change its value by accident.
}
