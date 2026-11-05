package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;

import java.util.List;

/** The player is sneaking. Use "negate" for "not sneaking". */
public final class SneakingCondition implements ConditionType {

    @Override
    public String id() {
        return "player.sneaking";
    }

    @Override
    public String displayName() {
        return "Sneaking";
    }

    @Override
    public String group() {
        return "Player";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of();
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        return Tri.of(ctx.screen().sneaking());
    }

    @Override
    public String describe(JsonObject params) {
        return "sneaking";
    }

}
