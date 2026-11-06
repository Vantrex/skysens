package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.ui.SearchField;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** TEXT, TEMPLATE_TEXT and INT parameters: a single line input; the spec description is the placeholder hint. */
public final class TextParamField extends ParamField {

    public static final int INPUT_H = 22;

    private final boolean number;
    private final SearchField input;
    private boolean initialising = true;

    public TextParamField(ParamSpec spec, JsonObject params, FieldContext ctx) {
        super(spec, params, ctx);
        this.number = spec.kind() == ParamKind.INT;
        this.input = new SearchField(this::onText).noHint().maxLength(this.number ? 9 : 256)
                .placeholder(spec.description() == null ? "" : spec.description());
        if (this.number) {
            this.input.charFilter(cp -> cp >= '0' && cp <= '9');
        }
        final String current = this.number ? String.valueOf(Params.getInt(params, spec.key(), defaultInt(spec))) : Params.getString(params, spec.key(), "");
        this.input.setText(current);
        this.input.setFocused(false);
        this.initialising = false;
    }

    private static int defaultInt(ParamSpec spec) {
        return spec.defaultValue() != null && spec.defaultValue().isJsonPrimitive() ? spec.defaultValue().getAsInt() : 0;
    }

    private void onText(String text) {
        if (this.initialising) {
            return;
        }
        if (this.number) {
            int value = 0;
            try {
                value = text.isEmpty() ? 0 : Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                // too long to parse, keep 0
            }
            this.params.addProperty(this.spec.key(), value);
        } else {
            this.params.addProperty(this.spec.key(), text);
        }
        this.changed();
    }

    @Override
    protected List<FormattedCharSequence> descriptionLines(int w) {
        return List.of();
    }

    @Override
    public int height(int w) {
        return LABEL_H + INPUT_H;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        FieldUi.label(g, this.spec.label(), x, y);
        this.input.layout(x, y + LABEL_H, w, INPUT_H);
        this.input.render(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return button == 0 && this.input.mouseClicked(mx, my);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        return this.input.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        return this.input.charTyped(e);
    }

    @Override
    public void blur() {
        this.input.setFocused(false);
    }

    @Override
    public boolean isTextFocused() {
        return this.input.isFocused();
    }

}
