package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.Supplier;

/**
 * ENUM, ENUM_MULTI, ZONE and CONFIG_TOGGLE parameters: a dropdown over the choices. Values are stored as strings
 * (enum constant names), so a stale name stays in the file and is shown in red.
 */
public final class ChoiceParamField extends ParamField {

    private final DropdownControl dropdown;

    private ChoiceParamField(ParamSpec spec, JsonObject params, FieldContext ctx, DropdownControl dropdown) {
        super(spec, params, ctx);
        this.dropdown = dropdown;
    }

    private static String defaultString(ParamSpec spec) {
        return spec.defaultValue() != null && spec.defaultValue().isJsonPrimitive() ? spec.defaultValue().getAsString() : "";
    }

    public static ChoiceParamField single(ParamSpec spec, JsonObject params, FieldContext ctx, Supplier<List<Choice>> choices) {
        final ChoiceParamField[] self = new ChoiceParamField[1];
        final DropdownControl dropdown = DropdownControl.single(choices,
                () -> Params.getString(params, spec.key(), defaultString(spec)),
                value -> {
                    params.addProperty(spec.key(), value);
                    self[0].changed();
                });
        self[0] = new ChoiceParamField(spec, params, ctx, dropdown);
        return self[0];
    }

    public static ChoiceParamField multi(ParamSpec spec, JsonObject params, FieldContext ctx, Supplier<List<Choice>> choices, String emptyLabel) {
        final ChoiceParamField[] self = new ChoiceParamField[1];
        final DropdownControl dropdown = DropdownControl.multi(choices,
                () -> Params.getStringList(params, spec.key()),
                values -> {
                    final JsonArray array = new JsonArray();
                    values.forEach(v -> array.add(new JsonPrimitive(v)));
                    params.add(spec.key(), array);
                    self[0].changed();
                }, emptyLabel);
        self[0] = new ChoiceParamField(spec, params, ctx, dropdown);
        return self[0];
    }

    @Override
    public int height(int w) {
        return this.headerHeight(w) + DropdownControl.HEIGHT;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        final int cy = this.renderHeader(g, x, y, w);
        this.dropdown.render(g, x, cy, w, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return button == 0 && this.dropdown.mouseClicked(mx, my);
    }

}
