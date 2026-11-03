package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** {@link ConfigEditorButton} on a {@link Runnable} field: a button that runs it. Has no value to reset. */
public class ButtonWidget extends CardWidget {

    private static final int PADDING_X = 8;
    private static final int HEIGHT = 16;

    public ButtonWidget(OptionNode node) {
        super(node);
    }

    private String label() {
        final ConfigEditorButton button = node.annotation(ConfigEditorButton.class);
        return button == null || button.buttonText().isBlank() ? "Open" : button.buttonText();
    }

    @Override
    protected int controlWidth() {
        return UiText.width(label(), UiText.Weight.SEMIBOLD) + 2 * PADDING_X;
    }

    @Override
    protected int controlHeight() {
        return HEIGHT;
    }

    private boolean overButton(double mx, double my) {
        return RenderUtil.inside(mx, my, controlX(), controlY(), controlWidth(), controlHeight());
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY, float partialTick) {
        final int w = controlWidth();
        final int bg = overButton(mouseX, mouseY) ? Theme.ACCENT_HOVER : Theme.ACCENT;
        RenderUtil.roundedRect(g, cx, cy, w, HEIGHT, 4, bg);
        UiText.draw(g, label(), cx + PADDING_X, cy + (HEIGHT - UiText.lineHeight()) / 2 + 1, 0xFFFFFFFF,
                UiText.Weight.SEMIBOLD);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button == 0 && overButton(mx, my) && node.get() instanceof Runnable action) {
            playClick();
            action.run();
            return true;
        }
        return false;
    }
}
