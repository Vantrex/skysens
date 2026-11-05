package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.MacroActivity;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.skyblock.diana.DianaTracker;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** What the player is doing: a dungeon, Kuudra, a slayer quest or Diana. */
public final class ActivityCondition implements ConditionType {

    @Override
    public String id() {
        return "activity";
    }

    @Override
    public String displayName() {
        return "Activity";
    }

    @Override
    public String group() {
        return "Activity";
    }

    @Override
    public List<ParamSpec> params() {
        final List<Choice> choices = new ArrayList<>();
        for (MacroActivity activity : MacroActivity.values()) {
            choices.add(new Choice(activity.name(), activity.displayName()));
        }
        return List.of(new ParamSpec("activity", "Activity", "What the player is doing", ParamKind.ENUM,
                new JsonPrimitive(MacroActivity.DUNGEON.name()), () -> choices, null));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final MacroActivity activity = Params.getEnum(params, "activity", MacroActivity.class, null);
        if (activity == null) {
            reasons.unknown("unknown activity");
            return Tri.UNKNOWN;
        }
        switch (activity) {
            case DUNGEON -> {
                final Tri result = ctx.dungeon().inRun(ctx.location());
                if (result == Tri.UNKNOWN) {
                    reasons.unknown("island not known yet");
                }
                return result;
            }
            case KUUDRA -> {
                final SkyblockLocationEnum island = ctx.location().island();
                if (island == null) {
                    reasons.unknown("island not known yet");
                    return Tri.UNKNOWN;
                }
                return Tri.of(island == SkyblockLocationEnum.KUUDRA);
            }
            case SLAYER -> {
                final Tri result = ctx.slayer().questActive();
                if (result == Tri.UNKNOWN) {
                    reasons.unknown("slayer state not known yet");
                }
                return result;
            }
            case DIANA -> {
                final Optional<Boolean> doing = DianaTracker.getInstance().getDoingDiana();
                if (doing.isEmpty()) {
                    reasons.unknown("Diana state not known yet");
                    return Tri.UNKNOWN;
                }
                return Tri.of(doing.get());
            }
        }
        return Tri.UNKNOWN;
    }

    @Override
    public String describe(JsonObject params) {
        final MacroActivity activity = Params.getEnum(params, "activity", MacroActivity.class, null);
        return "activity is " + (activity == null ? "?" : activity.displayName());
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getEnum(params, "activity", MacroActivity.class, null) == null) {
            problems.add("Unknown activity " + Params.getString(params, "activity", ""));
        }
    }

}
