package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.ui.SearchField;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

/**
 * TEXT_CHOICES parameter: a text input where any value can be typed, plus a small arrow that opens a list of suggestions
 * (mayor names, known pets, ...). Picking one writes it into the text.
 */
public final class TextSuggestParamField extends ParamField {

    private static final int ARROW_W = 22;

    private final SearchField input;
    private final DropdownControl suggestions;
    private boolean initialising = true;
    private int arrowX;
    private int arrowY;

    public TextSuggestParamField(ParamSpec spec, JsonObject params, FieldContext ctx, Supplier<List<Choice>> choices) {
        super(spec, params, ctx);
        this.input = new SearchField(this::onText).noHint().maxLength(64)
                .placeholder(spec.description() == null ? "" : spec.description());
        this.input.setText(Params.getString(params, spec.key(), ""));
        this.input.setFocused(false);
        this.suggestions = DropdownControl.single(choices, () -> Params.getString(params, spec.key(), ""), value -> this.input.setText(value));
        this.initialising = false;
    }

    private void onText(String text) {
        if (this.initialising) {
            return;
        }
        this.params.addProperty(this.spec.key(), text);
        this.changed();
    }

    @Override
    protected List<FormattedCharSequence> descriptionLines(int w) {
        return List.of();
    }

    @Override
    public int height(int w) {
        return LABEL_H + TextParamField.INPUT_H;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        FieldUi.label(g, this.spec.label(), x, y);
        this.input.layout(x, y + LABEL_H, w - ARROW_W - 4, TextParamField.INPUT_H);
        this.input.render(g, mx, my);
        this.arrowX = x + w - ARROW_W;
        this.arrowY = y + LABEL_H;
        this.suggestions.anchor(x, y + LABEL_H, w);
        final boolean hover = RenderUtil.inside(mx, my, this.arrowX, this.arrowY, ARROW_W, TextParamField.INPUT_H);
        RenderUtil.roundedRect(g, this.arrowX, this.arrowY, ARROW_W, TextParamField.INPUT_H, 4, hover ? Theme.CARD_HOVER : Theme.CARD);
        final int ax = this.arrowX + ARROW_W / 2 - 3;
        final int ay = this.arrowY + TextParamField.INPUT_H / 2;
        for (int i = 0; i < 4; i++) {
            RenderUtil.rect(g, ax + i, ay - 2 + i, 7 - 2 * i, 1, Theme.TEXT_MUTED);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        if (RenderUtil.inside(mx, my, this.arrowX, this.arrowY, ARROW_W, TextParamField.INPUT_H)) {
            this.input.setFocused(false);
            this.suggestions.openPopover();
            return true;
        }
        return this.input.mouseClicked(mx, my);
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
