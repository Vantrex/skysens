package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.List;
import java.util.Locale;

/** The title of the open container screen (chests, menus). FALSE when no container is open. */
public final class ContainerCondition implements ConditionType {

    public enum Op { EQUALS, CONTAINS }

    @Override
    public String id() {
        return "screen.container";
    }

    @Override
    public String displayName() {
        return "Open container";
    }

    @Override
    public String group() {
        return "Screen";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("op", "Check", "How to compare the title", ParamKind.ENUM, new JsonPrimitive(Op.CONTAINS.name()),
                        () -> List.of(new Choice("EQUALS", "equals"), new Choice("CONTAINS", "contains")), null),
                ParamSpec.of("text", "Title", "Container title text", ParamKind.TEXT, new JsonPrimitive("")),
                ParamSpec.of("ignoreCase", "Ignore case", "Compare without regard to upper/lower case", ParamKind.BOOL, new JsonPrimitive(true)));
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        String title = ctx.screen().containerTitle();
        if (title == null) {
            return Tri.FALSE;
        }
        String text = Params.getString(params, "text", "");
        if (Params.getBool(params, "ignoreCase", true)) {
            title = title.toLowerCase(Locale.ROOT);
            text = text.toLowerCase(Locale.ROOT);
        }
        return Tri.of(Params.getEnum(params, "op", Op.class, Op.CONTAINS) == Op.EQUALS ? title.equals(text) : title.contains(text));
    }

    @Override
    public String describe(JsonObject params) {
        return "container title " + Params.getString(params, "op", "CONTAINS").toLowerCase(Locale.ROOT) + " \"" + Params.getString(params, "text", "") + "\"";
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getString(params, "text", "").isEmpty()) {
            problems.add("No title text set");
        }
    }

}
