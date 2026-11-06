package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonArray;
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
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.Zone;
import de.vantrex.skysens.client.skyblock.location.zone.ZoneEnum;

import java.util.ArrayList;
import java.util.List;

/** The player is in one of the chosen zones of an island. */
public final class ZoneCondition implements ConditionType {

    /** The zones of an island (stored as zone enum constant names), empty for islands without zones. */
    public static List<Choice> zoneChoices(String islandName) {
        final List<Choice> out = new ArrayList<>();
        final SkyblockLocationEnum island = Params.parseEnum(SkyblockLocationEnum.class, islandName, null);
        if (island == null || island.getZoneEnum() == null) {
            return out;
        }
        for (Object constant : island.getZoneEnum().getEnumConstants()) {
            if (constant instanceof ZoneEnum<?> zone) {
                out.add(new Choice(zone.name(), zone.getDisplayName()));
            }
        }
        return out;
    }

    @Override
    public String id() {
        return "zone";
    }

    @Override
    public String displayName() {
        return "Zone";
    }

    @Override
    public String group() {
        return "Location";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("island", "Island", "The island the zones belong to", ParamKind.ENUM, new JsonPrimitive("HUB"),
                        IslandCondition::islandChoices, null),
                ParamSpec.of("zones", "Zones", "Matches in any of these zones. Empty = anywhere on the island", ParamKind.ZONE, new JsonArray()));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final SkyblockLocationEnum current = ctx.location().island();
        if (current == null) {
            reasons.unknown("island not known yet");
            return Tri.UNKNOWN;
        }
        if (!current.name().equals(Params.getString(params, "island", ""))) {
            return Tri.FALSE;
        }
        final List<String> zones = Params.getStringList(params, "zones");
        if (zones.isEmpty()) {
            return Tri.TRUE;
        }
        final Zone<?> zone = ctx.location().zone();
        if (zone == null) {
            reasons.unknown("zone not known yet");
            return Tri.UNKNOWN;
        }
        return Tri.of(zones.contains(zone.getZoneEnum().name()));
    }

    @Override
    public String describe(JsonObject params) {
        final String island = Params.getString(params, "island", "?");
        final SkyblockLocationEnum parsed = Params.parseEnum(SkyblockLocationEnum.class, island, null);
        final List<String> zones = Params.getStringList(params, "zones");
        return "zone is " + (zones.isEmpty() ? "any" : String.join(" or ", zones)) + " on " + (parsed == null ? island : parsed.getDisplayName());
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        final String island = Params.getString(params, "island", "");
        if (Params.parseEnum(SkyblockLocationEnum.class, island, null) == null) {
            problems.add("Unknown island " + island);
            return;
        }
        final List<String> valid = zoneChoices(island).stream().map(Choice::value).toList();
        for (String zone : Params.getStringList(params, "zones")) {
            if (!valid.contains(zone)) {
                problems.add("Unknown zone " + zone + " for " + island);
            }
        }
    }

}
