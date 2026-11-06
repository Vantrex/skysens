package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.DungeonFacts;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonClass;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonPartyMember;

import java.util.ArrayList;
import java.util.List;

/**
 * The dungeon class of the player (SELF), of any party member of the running dungeon including the player (ANY_MEMBER)
 * or of a named member (NAMED), optionally with a minimum class level. SELF also works outside a run (the selected class
 * is saved per profile). With {@code aliveOnly} dead members are ignored.
 */
public final class DungeonClassCondition implements ConditionType {

    public enum Who { SELF, ANY_MEMBER, NAMED }

    @Override
    public String id() {
        return "dungeon.class";
    }

    @Override
    public String displayName() {
        return "Dungeon class";
    }

    @Override
    public String group() {
        return "Dungeon";
    }

    private static boolean is(JsonObject p, Who who) {
        return who.name().equals(Params.getString(p, "who", Who.SELF.name()));
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("who", "Whose class", "SELF: your selected class. ANY_MEMBER: any member of the running dungeon (including you). NAMED: one member",
                        ParamKind.ENUM, new JsonPrimitive(Who.SELF.name()),
                        () -> List.of(new Choice("SELF", "Me"), new Choice("ANY_MEMBER", "Any party member"), new Choice("NAMED", "A named member")), null),
                new ParamSpec("name", "Player name", "The party member to check", ParamKind.TEXT, new JsonPrimitive(""), null, p -> is(p, Who.NAMED)),
                new ParamSpec("classes", "Classes", "Matches any of these classes. None = any class", ParamKind.ENUM_MULTI, new JsonArray(),
                        DungeonFacts::classChoices, null),
                ParamSpec.of("minLevel", "Minimum class level", "0 = any level", ParamKind.INT, new JsonPrimitive(0)),
                new ParamSpec("aliveOnly", "Alive members only", "Ignore members who are dead", ParamKind.BOOL, new JsonPrimitive(false),
                        null, p -> !is(p, Who.SELF)));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final Who who = Params.getEnum(params, "who", Who.class, null);
        if (who == null) {
            reasons.unknown("unknown dungeon class check");
            return Tri.UNKNOWN;
        }
        final DungeonFacts dungeon = ctx.dungeon();
        final List<String> classes = Params.getStringList(params, "classes");
        final int minLevel = Params.getInt(params, "minLevel", 0);
        if (who == Who.SELF) {
            final DungeonClass own = dungeon.ownClass();
            if (own == null) {
                reasons.unknown("own dungeon class not known yet");
                return Tri.UNKNOWN;
            }
            return matches(own, dungeon.ownLevel(), classes, minLevel, reasons, "own class level");
        }
        final Tri inRun = dungeon.inRun(ctx.location());
        if (inRun != Tri.TRUE) {
            if (inRun == Tri.UNKNOWN) {
                reasons.unknown("island not known yet");
            }
            return inRun;
        }
        final boolean aliveOnly = Params.getBool(params, "aliveOnly", false);
        final List<DungeonPartyMember> members = dungeon.members();
        if (members.isEmpty()) {
            reasons.unknown("dungeon party not read yet");
            return Tri.UNKNOWN;
        }
        if (who == Who.NAMED) {
            final String name = Params.getString(params, "name", "").trim();
            final DungeonPartyMember member = dungeon.member(name);
            if (member == null || (aliveOnly && !member.alive())) {
                return Tri.FALSE;
            }
            return memberMatch(member, classes, minLevel, reasons);
        }
        boolean unknown = false;
        for (DungeonPartyMember member : members) {
            if (aliveOnly && !member.alive()) {
                continue;
            }
            final Tri result = memberMatch(member, classes, minLevel, reasons);
            if (result == Tri.TRUE) {
                return Tri.TRUE;
            }
            unknown |= result == Tri.UNKNOWN;
        }
        return unknown ? Tri.UNKNOWN : Tri.FALSE;
    }

    private static Tri memberMatch(DungeonPartyMember member, List<String> classes, int minLevel, ReasonSink reasons) {
        if (member.dungeonClass() == null) {
            reasons.unknown("class of " + member.name() + " not known");
            return Tri.UNKNOWN;
        }
        return matches(member.dungeonClass(), member.level(), classes, minLevel, reasons, "class level of " + member.name());
    }

    private static Tri matches(DungeonClass actual, int level, List<String> classes, int minLevel, ReasonSink reasons, String levelName) {
        if (!classes.isEmpty() && !classes.contains(actual.name())) {
            return Tri.FALSE;
        }
        if (minLevel > 0) {
            if (level <= 0) {
                reasons.unknown(levelName + " not known");
                return Tri.UNKNOWN;
            }
            return Tri.of(level >= minLevel);
        }
        return Tri.TRUE;
    }

    @Override
    public String describe(JsonObject params) {
        final Who who = Params.getEnum(params, "who", Who.class, Who.SELF);
        final List<String> classes = new ArrayList<>();
        Params.getStringList(params, "classes").forEach(c -> classes.add(DungeonFacts.classDisplayName(c)));
        final int min = Params.getInt(params, "minLevel", 0);
        final String subject = switch (who) {
            case SELF -> "my class";
            case ANY_MEMBER -> "a party member's class";
            case NAMED -> Params.getString(params, "name", "?") + "'s class";
        };
        return subject + " is " + (classes.isEmpty() ? "any" : String.join(" or ", classes)) + (min > 0 ? " level " + min + "+" : "");
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        final Who who = Params.getEnum(params, "who", Who.class, null);
        if (who == null) {
            problems.add("Unknown selection " + Params.getString(params, "who", ""));
        } else if (who == Who.NAMED && Params.getString(params, "name", "").isBlank()) {
            problems.add("No player name set");
        }
        for (String name : Params.getStringList(params, "classes")) {
            if (!DungeonFacts.isClass(name)) {
                problems.add("Unknown dungeon class " + name);
            }
        }
    }

}
