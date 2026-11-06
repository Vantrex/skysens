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
import de.vantrex.skysens.client.skyblock.time.SkyblockCalendarEvent;

import java.util.ArrayList;
import java.util.List;

/** A calendar event with a fixed window (Spooky Festival, Season of Jerry, New Year, Traveling Zoo) is running. Never UNKNOWN. */
public final class CalendarEventCondition implements ConditionType {

    private static List<Choice> choices() {
        final List<Choice> out = new ArrayList<>();
        for (SkyblockCalendarEvent event : SkyblockCalendarEvent.values()) {
            out.add(new Choice(event.name(), event.getDisplayName()));
        }
        return out;
    }

    @Override
    public String id() {
        return "calendar.event";
    }

    @Override
    public String displayName() {
        return "Calendar event";
    }

    @Override
    public String group() {
        return "Time";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(new ParamSpec("events", "Events", "Matches while any of these events is on (computed from the SkyBlock calendar)",
                ParamKind.ENUM_MULTI, new JsonArray(), CalendarEventCondition::choices, null));
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        for (String name : Params.getStringList(params, "events")) {
            final SkyblockCalendarEvent event = Params.parseEnum(SkyblockCalendarEvent.class, name, null);
            if (event != null && ctx.time().eventActive(event)) {
                return Tri.TRUE;
            }
        }
        return Tri.FALSE;
    }

    @Override
    public String describe(JsonObject params) {
        final List<String> names = new ArrayList<>();
        for (String name : Params.getStringList(params, "events")) {
            final SkyblockCalendarEvent event = Params.parseEnum(SkyblockCalendarEvent.class, name, null);
            names.add(event == null ? name : event.getDisplayName());
        }
        return (names.isEmpty() ? "(no event)" : String.join(" or ", names)) + " is on";
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        final List<String> events = Params.getStringList(params, "events");
        if (events.isEmpty()) {
            problems.add("No event chosen");
        }
        for (String name : events) {
            if (Params.parseEnum(SkyblockCalendarEvent.class, name, null) == null) {
                problems.add("Unknown event " + name);
            }
        }
    }

}
