package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.DungeonFacts;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.List;

/** The floor of the running dungeon (F1-F7, M1-M7) is one of the chosen ones. FALSE outside a run. */
public final class DungeonFloorCondition implements ConditionType {

    @Override
    public String id() {
        return "dungeon.floor";
    }

    @Override
    public String displayName() {
        return "Dungeon floor";
    }

    @Override
    public String group() {
        return "Dungeon";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(new ParamSpec("floors", "Floors", "Matches when the running dungeon is on any of these floors", ParamKind.ENUM_MULTI,
                new JsonArray(), DungeonFacts::floorChoices, null));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final DungeonFacts dungeon = ctx.dungeon();
        final Tri inRun = dungeon.inRun(ctx.location());
        if (inRun != Tri.TRUE) {
            if (inRun == Tri.UNKNOWN) {
                reasons.unknown("island not known yet");
            }
            return inRun;
        }
        final String floor = dungeon.floor();
        if (floor == null) {
            reasons.unknown("dungeon floor not read yet");
            return Tri.UNKNOWN;
        }
        return Tri.of(Params.getStringList(params, "floors").contains(floor));
    }

    @Override
    public String describe(JsonObject params) {
        final List<String> floors = Params.getStringList(params, "floors");
        return "dungeon floor is " + (floors.isEmpty() ? "(none)" : String.join(" or ", floors));
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        final List<String> floors = Params.getStringList(params, "floors");
        if (floors.isEmpty()) {
            problems.add("No floor chosen");
        }
        final List<String> valid = DungeonFacts.floorChoices().stream().map(Choice::value).toList();
        for (String floor : floors) {
            if (!valid.contains(floor)) {
                problems.add("Unknown floor " + floor);
            }
        }
    }

}
