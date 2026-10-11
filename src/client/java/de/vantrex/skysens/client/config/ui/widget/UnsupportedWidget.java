package de.vantrex.skysens.client.config.ui.widget;

import de.vantrex.skysens.client.config.ui.model.OptionNode;
import de.vantrex.skysens.client.config.ui.render.RenderUtil;
import de.vantrex.skysens.client.config.ui.render.Theme;
import de.vantrex.skysens.client.config.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

/** Placeholder card for option kinds the custom GUI cannot edit. */
public class UnsupportedWidget extends CardWidget {

    private static final String LABEL = "Not supported";

    public UnsupportedWidget(OptionNode node) {
        super(node);
    }

    @Override
    protected int controlWidth() {
        return UiText.width(LABEL) + 10;
    }

    @Override
    protected int controlHeight() {
        return 14;
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY, float partialTick) {
        int w = controlWidth();
        int bg = ARGB.color(0x26, (Theme.DANGER >> 16) & 0xFF, (Theme.DANGER >> 8) & 0xFF, Theme.DANGER & 0xFF);
        RenderUtil.roundedRect(g, cx, cy, w, controlHeight(), 4, bg);
        UiText.draw(g, LABEL, cx + 5, cy + (controlHeight() - UiText.lineHeight()) / 2 + 1,
                Theme.DANGER);
    }
}
