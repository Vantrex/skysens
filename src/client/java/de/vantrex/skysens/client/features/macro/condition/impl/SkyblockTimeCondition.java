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
import de.vantrex.skysens.client.skyblock.time.SkyblockDate;
import de.vantrex.skysens.client.skyblock.time.SkyblockMonth;

import java.util.ArrayList;
import java.util.List;

/**
 * The SkyBlock clock: month, season, day range and hour range (all inclusive; a range with from > to wraps around, e.g.
 * hours 22 to 2). Never UNKNOWN. The offset between the in-game clock and the computed hour is not verified.
 */
public final class SkyblockTimeCondition implements ConditionType {

    public static List<Choice> monthChoices() {
        final List<Choice> out = new ArrayList<>();
        for (SkyblockMonth month : SkyblockMonth.values()) {
            out.add(new Choice(month.name(), month.getDisplayName()));
        }
        return out;
    }

    public static List<Choice> seasonChoices() {
        final List<Choice> out = new ArrayList<>();
        for (SkyblockMonth.Season season : SkyblockMonth.Season.values()) {
            out.add(new Choice(season.name(), season.name().charAt(0) + season.name().substring(1).toLowerCase()));
        }
        return out;
    }

    @Override
    public String id() {
        return "time.skyblock";
    }

    @Override
    public String displayName() {
        return "SkyBlock time";
    }

    @Override
    public String group() {
        return "Time";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("months", "Months", "Any of these months. None = any month", ParamKind.ENUM_MULTI, new JsonArray(),
                        SkyblockTimeCondition::monthChoices, null),
                new ParamSpec("seasons", "Seasons", "Any of these seasons. None = any season", ParamKind.ENUM_MULTI, new JsonArray(),
                        SkyblockTimeCondition::seasonChoices, null),
                ParamSpec.of("fromDay", "From day", "Day of the month, 1-31", ParamKind.INT, new JsonPrimitive(1)),
                ParamSpec.of("toDay", "To day", "Day of the month, 1-31 (from > to wraps around the month end)", ParamKind.INT, new JsonPrimitive(31)),
                ParamSpec.of("fromHour", "From hour", "SkyBlock hour, 0-23", ParamKind.INT, new JsonPrimitive(0)),
                ParamSpec.of("toHour", "To hour", "SkyBlock hour, 0-23 (from > to wraps around midnight)", ParamKind.INT, new JsonPrimitive(23)));
    }

    private static boolean inRange(int value, int from, int to) {
        return from <= to ? value >= from && value <= to : value >= from || value <= to;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final SkyblockDate date = ctx.time().date();
        final List<String> months = Params.getStringList(params, "months");
        if (!months.isEmpty() && !months.contains(date.month().name())) {
            return Tri.FALSE;
        }
        final List<String> seasons = Params.getStringList(params, "seasons");
        if (!seasons.isEmpty() && !seasons.contains(date.month().getSeason().name())) {
            return Tri.FALSE;
        }
        if (!inRange(date.day(), Params.getInt(params, "fromDay", 1), Params.getInt(params, "toDay", 31))) {
            return Tri.FALSE;
        }
        return Tri.of(inRange(date.hour(), Params.getInt(params, "fromHour", 0), Params.getInt(params, "toHour", 23)));
    }

    @Override
    public String describe(JsonObject params) {
        final List<String> parts = new ArrayList<>();
        final List<String> months = Params.getStringList(params, "months");
        if (!months.isEmpty()) {
            final List<String> names = new ArrayList<>();
            months.forEach(m -> {
                final SkyblockMonth month = Params.parseEnum(SkyblockMonth.class, m, null);
                names.add(month == null ? m : month.getDisplayName());
            });
            parts.add(String.join("/", names));
        }
        final List<String> seasons = Params.getStringList(params, "seasons");
        if (!seasons.isEmpty()) {
            parts.add(String.join("/", seasons).toLowerCase());
        }
        final int fromDay = Params.getInt(params, "fromDay", 1);
        final int toDay = Params.getInt(params, "toDay", 31);
        if (fromDay != 1 || toDay != 31) {
            parts.add("day " + fromDay + "-" + toDay);
        }
        final int fromHour = Params.getInt(params, "fromHour", 0);
        final int toHour = Params.getInt(params, "toHour", 23);
        if (fromHour != 0 || toHour != 23) {
            parts.add("hour " + fromHour + "-" + toHour);
        }
        return "SkyBlock time" + (parts.isEmpty() ? " (any)" : " " + String.join(", ", parts));
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        for (String month : Params.getStringList(params, "months")) {
            if (Params.parseEnum(SkyblockMonth.class, month, null) == null) {
                problems.add("Unknown month " + month);
            }
        }
        for (String season : Params.getStringList(params, "seasons")) {
            if (Params.parseEnum(SkyblockMonth.Season.class, season, null) == null) {
                problems.add("Unknown season " + season);
            }
        }
        final int fromDay = Params.getInt(params, "fromDay", 1);
        final int toDay = Params.getInt(params, "toDay", 31);
        if (fromDay < 1 || fromDay > 31 || toDay < 1 || toDay > 31) {
            problems.add("Days must be between 1 and 31");
        }
        final int fromHour = Params.getInt(params, "fromHour", 0);
        final int toHour = Params.getInt(params, "toHour", 23);
        if (fromHour < 0 || fromHour > 23 || toHour < 0 || toHour > 23) {
            problems.add("Hours must be between 0 and 23");
        }
    }

}
