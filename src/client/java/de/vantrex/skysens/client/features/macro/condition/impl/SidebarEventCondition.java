package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.facts.SidebarFacts;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.skyblock.sidebar.SidebarEventType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Events shown in the sidebar: the mining event, Jacob's contest (optionally for a crop), the Kuudra tier and the timed
 * Spooky Festival, Traveling Zoo and New Year events. UNKNOWN until the sidebar was read since the last join or island
 * change; Jacob's crop is UNKNOWN while a contest is on but its crop is not visible.
 */
public final class SidebarEventCondition implements ConditionType {

    public enum Check { MINING_EVENT, JACOBS_CONTEST, KUUDRA, SPOOKY_FESTIVAL, TRAVELING_ZOO, NEW_YEAR }

    private static final List<String> MINING_EVENTS = List.of("Gone with the Wind", "Better Together", "Double Powder", "Goblin Raid", "Raffle");
    private static final List<String> CROPS = List.of("Wheat", "Carrot", "Potato", "Pumpkin", "Melon", "Mushroom", "Cocoa Beans",
            "Cactus", "Sugar Cane", "Nether Wart");

    @Override
    public String id() {
        return "sidebar.event";
    }

    @Override
    public String displayName() {
        return "Sidebar event";
    }

    @Override
    public String group() {
        return "Activity";
    }

    private static boolean is(JsonObject p, Check check) {
        return check.name().equals(Params.getString(p, "check", Check.MINING_EVENT.name()));
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("check", "Event", "Which sidebar event to look for", ParamKind.ENUM, new JsonPrimitive(Check.MINING_EVENT.name()),
                        () -> List.of(new Choice("MINING_EVENT", "Mining event"), new Choice("JACOBS_CONTEST", "Jacob's contest"),
                                new Choice("KUUDRA", "Kuudra tier"), new Choice("SPOOKY_FESTIVAL", "Spooky Festival"),
                                new Choice("TRAVELING_ZOO", "Traveling Zoo"), new Choice("NEW_YEAR", "New Year event")), null),
                new ParamSpec("name", "Mining event", "Event name as shown in the sidebar (contains). Empty = any mining event",
                        ParamKind.TEXT_CHOICES, new JsonPrimitive(""),
                        () -> MINING_EVENTS.stream().map(e -> new Choice(e, e)).toList(), p -> is(p, Check.MINING_EVENT)),
                new ParamSpec("crop", "Crop", "Only when the contest is for this crop. Empty = any crop", ParamKind.TEXT_CHOICES,
                        new JsonPrimitive(""), () -> CROPS.stream().map(e -> new Choice(e, e)).toList(), p -> is(p, Check.JACOBS_CONTEST)),
                ParamSpec.of("minTier", "Minimum tier", "Kuudra tier 1-5. 0 = any tier", ParamKind.INT, new JsonPrimitive(0)));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final Check check = Params.getEnum(params, "check", Check.class, null);
        if (check == null) {
            reasons.unknown("unknown sidebar event check");
            return Tri.UNKNOWN;
        }
        final SidebarFacts sidebar = ctx.sidebar();
        if (!sidebar.known()) {
            reasons.unknown("sidebar not read yet");
            return Tri.UNKNOWN;
        }
        switch (check) {
            case MINING_EVENT -> {
                final String event = sidebar.miningEvent();
                final String wanted = Params.getString(params, "name", "").trim();
                if (event == null) {
                    return Tri.FALSE;
                }
                return Tri.of(wanted.isEmpty() || event.toLowerCase(Locale.ROOT).contains(wanted.toLowerCase(Locale.ROOT)));
            }
            case JACOBS_CONTEST -> {
                final Tri active = sidebar.jacobsActive();
                final String crop = Params.getString(params, "crop", "").trim();
                if (active != Tri.TRUE || crop.isEmpty()) {
                    return active;
                }
                final Optional<String> shown = sidebar.jacobsCrop();
                if (shown.isEmpty()) {
                    reasons.unknown("Jacob's contest crop not visible in the sidebar");
                    return Tri.UNKNOWN;
                }
                return Tri.of(shown.get().toLowerCase(Locale.ROOT).contains(crop.toLowerCase(Locale.ROOT)));
            }
            case KUUDRA -> {
                return Tri.of(sidebar.kuudraTier() >= Math.max(1, Params.getInt(params, "minTier", 0)));
            }
            case SPOOKY_FESTIVAL -> {
                return sidebar.timedEventActive(SidebarEventType.SPOOKY_FESTIVAL);
            }
            case TRAVELING_ZOO -> {
                return sidebar.timedEventActive(SidebarEventType.TRAVELING_ZOO);
            }
            default -> {
                return sidebar.timedEventActive(SidebarEventType.NEW_YEAR);
            }
        }
    }

    @Override
    public String describe(JsonObject params) {
        final Check check = Params.getEnum(params, "check", Check.class, null);
        if (check == null) {
            return "unknown sidebar event";
        }
        return switch (check) {
            case MINING_EVENT -> Params.getString(params, "name", "").isBlank() ? "a mining event is on"
                    : "mining event " + Params.getString(params, "name", "").trim();
            case JACOBS_CONTEST -> Params.getString(params, "crop", "").isBlank() ? "Jacob's contest is on"
                    : "Jacob's contest for " + Params.getString(params, "crop", "").trim();
            case KUUDRA -> Params.getInt(params, "minTier", 0) > 0 ? "in Kuudra tier " + Params.getInt(params, "minTier", 0) + "+" : "in Kuudra";
            case SPOOKY_FESTIVAL -> "Spooky Festival is on (sidebar)";
            case TRAVELING_ZOO -> "Traveling Zoo is on (sidebar)";
            case NEW_YEAR -> "New Year event is on (sidebar)";
        };
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getEnum(params, "check", Check.class, null) == null) {
            problems.add("Unknown sidebar event " + Params.getString(params, "check", ""));
        }
        final int tier = Params.getInt(params, "minTier", 0);
        if (tier < 0 || tier > 5) {
            problems.add("Kuudra tier must be between 0 and 5");
        }
    }

}
