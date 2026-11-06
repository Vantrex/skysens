package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** BOOL parameter: label on the left, knob toggle on the right. */
public final class BoolParamField extends ParamField {

    private static final int ROW_H = 18;

    private int x;
    private int y;
    private int w;

    public BoolParamField(ParamSpec spec, JsonObject params, FieldContext ctx) {
        super(spec, params, ctx);
    }

    private boolean value() {
        final boolean def = this.spec.defaultValue() != null && this.spec.defaultValue().isJsonPrimitive() && this.spec.defaultValue().getAsBoolean();
        return Params.getBool(this.params, this.spec.key(), def);
    }

    private int descHeight(int w) {
        final List<FormattedCharSequence> lines = this.descriptionLines(w - FieldUi.TOGGLE_W - 10);
        return lines.isEmpty() ? 0 : lines.size() * (UiText.lineHeight() + 1);
    }

    @Override
    public int height(int w) {
        return ROW_H + this.descHeight(w);
    }

    @Override
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        this.x = x;
        this.y = y;
        this.w = w;
        final boolean on = this.value();
        UiText.draw(g, UiText.ellipsize(this.spec.label(), w - FieldUi.TOGGLE_W - 8), x, y + (ROW_H - 9) / 2, Theme.TEXT);
        FieldUi.toggle(g, x + w - FieldUi.TOGGLE_W, y + (ROW_H - FieldUi.TOGGLE_H) / 2, on);
        int dy = y + ROW_H - 2;
        for (FormattedCharSequence line : this.descriptionLines(w - FieldUi.TOGGLE_W - 10)) {
            UiText.drawLine(g, line, x, dy, Theme.TEXT_FAINT);
            dy += UiText.lineHeight() + 1;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && RenderUtil.inside(mx, my, this.x, this.y, this.w, ROW_H)) {
            this.params.addProperty(this.spec.key(), !this.value());
            OptionWidget.playClick();
            this.changed();
            return true;
        }
        return false;
    }

}
