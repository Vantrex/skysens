package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

/** The fields generated from a list of param specs, stacked vertically. Fields hidden by visibleIf take no space. */
public final class ParamFieldList {

    public static final int GAP = 8;

    private final List<ParamSpec> specs;
    private final JsonObject params;
    private final List<ParamField> fields = new ArrayList<>();
    private final List<ParamField> laidOut = new ArrayList<>();
    private int[] tops = new int[0];
    private int[] heights = new int[0];

    public ParamFieldList(List<ParamSpec> specs, JsonObject params, FieldContext ctx) {
        this.specs = specs;
        this.params = params;
        for (ParamSpec spec : specs) {
            this.fields.add(ParamFields.create(spec, params, ctx));
        }
    }

    private List<ParamField> visible() {
        final List<ParamField> out = new ArrayList<>();
        for (int i = 0; i < this.fields.size(); i++) {
            final var visibleIf = this.specs.get(i).visibleIf();
            if (visibleIf == null || visibleIf.test(this.params)) {
                out.add(this.fields.get(i));
            }
        }
        return out;
    }

    public int height(int w) {
        int h = 0;
        for (ParamField field : this.visible()) {
            h += (h == 0 ? 0 : GAP) + field.height(w);
        }
        return h;
    }

    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        this.laidOut.clear();
        final List<ParamField> visible = this.visible();
        this.tops = new int[visible.size()];
        this.heights = new int[visible.size()];
        int cy = y;
        for (int i = 0; i < visible.size(); i++) {
            final ParamField field = visible.get(i);
            final int h = field.height(w);
            this.laidOut.add(field);
            this.tops[i] = cy;
            this.heights[i] = h;
            field.render(g, x, cy, w, mx, my);
            cy += h + GAP;
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        this.blur();
        for (int i = 0; i < this.laidOut.size(); i++) {
            if (my >= this.tops[i] && my < this.tops[i] + this.heights[i] && this.laidOut.get(i).mouseClicked(mx, my, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyEvent e) {
        for (ParamField field : this.fields) {
            if (field.isTextFocused() && field.keyPressed(e)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent e) {
        for (ParamField field : this.fields) {
            if (field.isTextFocused() && field.charTyped(e)) {
                return true;
            }
        }
        return false;
    }

    public void blur() {
        this.fields.forEach(ParamField::blur);
    }

    public boolean isTextFocused() {
        for (ParamField field : this.fields) {
            if (field.isTextFocused()) {
                return true;
            }
        }
        return false;
    }

}
