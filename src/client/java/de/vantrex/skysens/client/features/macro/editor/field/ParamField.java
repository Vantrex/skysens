package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Editor for one parameter of a node or condition. Reads and writes the JSON params object directly. */
public abstract class ParamField {

    protected static final int LABEL_H = 12;

    protected final ParamSpec spec;
    protected final JsonObject params;
    protected final FieldContext ctx;

    protected ParamField(ParamSpec spec, JsonObject params, FieldContext ctx) {
        this.spec = spec;
        this.params = params;
        this.ctx = ctx;
    }

    public abstract int height(int w);

    public abstract void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean keyPressed(KeyEvent e) {
        return false;
    }

    public boolean charTyped(CharacterEvent e) {
        return false;
    }

    /** Drops text focus (recursively for containers). */
    public void blur() {
    }

    public boolean isTextFocused() {
        return false;
    }

    protected void changed() {
        this.ctx.changed().accept(this.spec.key());
    }

    // ------------------------------------------------------------------ shared label / description layout

    protected List<FormattedCharSequence> descriptionLines(int w) {
        if (!this.ctx.describe() || this.spec.description() == null || this.spec.description().isBlank()) {
            return List.of();
        }
        return UiText.wrap(this.spec.description(), w);
    }

    /** Height of the label row plus the description lines. */
    protected int headerHeight(int w) {
        final int lines = this.descriptionLines(w).size();
        return LABEL_H + (lines == 0 ? 0 : lines * (UiText.lineHeight() + 1) + 2);
    }

    /** Draws label and description; returns the y where the control starts. */
    protected int renderHeader(GuiGraphicsExtractor g, int x, int y, int w) {
        FieldUi.label(g, this.spec.label(), x, y);
        int cy = y + LABEL_H;
        final List<FormattedCharSequence> lines = this.descriptionLines(w);
        if (!lines.isEmpty()) {
            // description sits under the label, above the control
            cy = y + 11;
            for (FormattedCharSequence line : lines) {
                UiText.drawLine(g, line, x, cy, Theme.TEXT_FAINT);
                cy += UiText.lineHeight() + 1;
            }
            cy += 3;
        }
        return cy;
    }


}
