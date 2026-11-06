package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;

import java.util.ArrayList;
import java.util.List;

/** The current island is one of the chosen ones. */
public final class IslandCondition implements ConditionType {

    public static List<Choice> islandChoices() {
        final List<Choice> out = new ArrayList<>();
        for (SkyblockLocationEnum island : SkyblockLocationEnum.values()) {
            out.add(new Choice(island.name(), island.getDisplayName()));
        }
        return out;
    }

    @Override
    public String id() {
        return "island";
    }

    @Override
    public String displayName() {
        return "Island";
    }

    @Override
    public String group() {
        return "Location";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(new ParamSpec("islands", "Islands", "Matches when the current island is any of these", ParamKind.ENUM_MULTI,
                new JsonArray(), IslandCondition::islandChoices, null));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final SkyblockLocationEnum island = ctx.location().island();
        if (island == null) {
            reasons.unknown("island not known yet");
            return Tri.UNKNOWN;
        }
        return Tri.of(Params.getStringList(params, "islands").contains(island.name()));
    }

    @Override
    public String describe(JsonObject params) {
        final List<String> names = new ArrayList<>();
        for (String name : Params.getStringList(params, "islands")) {
            final SkyblockLocationEnum island = Params.parseEnum(SkyblockLocationEnum.class, name, null);
            names.add(island == null ? name : island.getDisplayName());
        }
        return "island is " + (names.isEmpty() ? "(none)" : String.join(" or ", names));
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        final List<String> islands = Params.getStringList(params, "islands");
        if (islands.isEmpty()) {
            problems.add("No island chosen");
        }
        for (String name : islands) {
            if (Params.parseEnum(SkyblockLocationEnum.class, name, null) == null) {
                problems.add("Unknown island " + name);
            }
        }
    }

}
