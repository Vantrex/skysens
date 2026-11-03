package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.Animated;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Boolean option rendered as a pill toggle. Clicking anywhere on the card toggles it.
 */
public class ToggleWidget extends CardWidget {

    private final Animated knob;

    public ToggleWidget(OptionNode node) {
        super(node);
        this.knob = new Animated(isOn() ? 1f : 0f, 150f);
    }

    private boolean isOn() {
        return node.get() instanceof Boolean b && b;
    }

    @Override
    protected int controlWidth() {
        return Theme.style().toggleW();
    }

    @Override
    protected int controlHeight() {
        return Theme.style().toggleH();
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY,
                                 float partialTick) {
        int w = Theme.style().toggleW();
        int h = Theme.style().toggleH();
        knob.target(isOn() ? 1f : 0f);
        float t = knob.value();
        RenderUtil.roundedRect(g, cx, cy, w, h, h / 2, RenderUtil.lerpColor(t, Theme.TRACK, Theme.ACCENT));
        int r = (h - 4) / 2;
        RenderUtil.knob(g, cx + 2 + r + Math.round(t * (w - h)), cy + h / 2, r, t);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button == 0 && isHovered(mx, my)) {
            node.set(Boolean.valueOf(!isOn()));
            playClick();
            return true;
        }
        return false;
    }
}
