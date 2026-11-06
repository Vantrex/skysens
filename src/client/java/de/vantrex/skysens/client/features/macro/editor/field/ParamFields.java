package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.condition.impl.ZoneCondition;
import de.vantrex.skysens.client.features.macro.node.action.ToggleOptionNode;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.List;
import java.util.function.Supplier;

/** Creates the field for a param spec. */
public final class ParamFields {

    private static List<Choice> configToggles;

    private ParamFields() {
    }

    private static synchronized List<Choice> configToggles() {
        if (configToggles == null) {
            configToggles = ToggleOptionNode.choices();
        }
        return configToggles;
    }

    public static ParamField create(ParamSpec spec, JsonObject params, FieldContext ctx) {
        final Supplier<List<Choice>> choices = spec.choices() != null ? spec.choices() : List::of;
        return switch (spec.kind()) {
            case TEXT, TEMPLATE_TEXT, INT -> new TextParamField(spec, params, ctx);
            case BOOL -> new BoolParamField(spec, params, ctx);
            case ENUM -> ChoiceParamField.single(spec, params, ctx, choices);
            case ENUM_MULTI -> ChoiceParamField.multi(spec, params, ctx, choices, "None selected");
            case ZONE -> ChoiceParamField.multi(spec, params, ctx,
                    () -> ZoneCondition.zoneChoices(Params.getString(params, "island", "")), "Anywhere on the island");
            case CONFIG_TOGGLE -> ChoiceParamField.single(spec, params, ctx, ParamFields::configToggles);
            case KEYBIND -> new KeybindParamField(spec, params, ctx);
            case RULE_LIST -> new RuleListParamField(spec, params, ctx);
        };
    }

}
