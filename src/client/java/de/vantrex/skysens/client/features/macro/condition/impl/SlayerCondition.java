package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.facts.SlayerFacts;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.List;
import java.util.Locale;

/** The slayer quest state, optionally for a given boss and minimum tier. */
public final class SlayerCondition implements ConditionType {

    public enum State { ANY_ACTIVE, QUEST_ACTIVE, BOSS_SPAWNED, NONE }

    @Override
    public String id() {
        return "slayer";
    }

    @Override
    public String displayName() {
        return "Slayer";
    }

    @Override
    public String group() {
        return "Activity";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("state", "State", "ANY_ACTIVE: a quest is running. QUEST_ACTIVE: running, boss not spawned yet. BOSS_SPAWNED: the boss is out. NONE: no quest",
                        ParamKind.ENUM, new JsonPrimitive(State.ANY_ACTIVE.name()),
                        () -> List.of(new Choice("ANY_ACTIVE", "Any quest running"), new Choice("QUEST_ACTIVE", "Quest running, boss not spawned"),
                                new Choice("BOSS_SPAWNED", "Boss spawned"), new Choice("NONE", "No quest")), null),
                ParamSpec.of("boss", "Boss", "Boss name, e.g. Revenant Horror. Empty = any", ParamKind.TEXT, new JsonPrimitive("")),
                ParamSpec.of("minTier", "Minimum tier", "0 = any", ParamKind.INT, new JsonPrimitive(0)));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final State state = Params.getEnum(params, "state", State.class, State.ANY_ACTIVE);
        final SlayerFacts slayer = ctx.slayer();
        final Tri active = slayer.questActive();
        if (active == Tri.UNKNOWN) {
            reasons.unknown("slayer state not known yet");
            return Tri.UNKNOWN;
        }
        if (state == State.NONE) {
            return active == Tri.TRUE ? Tri.FALSE : Tri.TRUE;
        }
        if (active == Tri.FALSE) {
            return Tri.FALSE;
        }
        final Tri phase = switch (state) {
            case BOSS_SPAWNED -> slayer.bossSpawned();
            case QUEST_ACTIVE -> slayer.bossSpawned().not();
            default -> Tri.TRUE;
        };
        if (phase != Tri.TRUE) {
            return phase;
        }
        final String boss = Params.getString(params, "boss", "").trim();
        final int minTier = Params.getInt(params, "minTier", 0);
        if (boss.isEmpty() && minTier <= 0) {
            return Tri.TRUE;
        }
        final String bossName = slayer.bossName();
        if (bossName == null) {
            reasons.unknown("slayer boss not read from the sidebar yet");
            return Tri.UNKNOWN;
        }
        if (!boss.isEmpty() && !bossName.toLowerCase(Locale.ROOT).contains(boss.toLowerCase(Locale.ROOT))) {
            return Tri.FALSE;
        }
        if (minTier > 0) {
            final int tier = slayer.tier();
            if (tier == 0) {
                reasons.unknown("slayer tier not read from the sidebar yet");
                return Tri.UNKNOWN;
            }
            return Tri.of(tier >= minTier);
        }
        return Tri.TRUE;
    }

    @Override
    public String describe(JsonObject params) {
        final String boss = Params.getString(params, "boss", "").trim();
        final int minTier = Params.getInt(params, "minTier", 0);
        return "slayer " + Params.getString(params, "state", "ANY_ACTIVE").toLowerCase(Locale.ROOT).replace('_', ' ')
                + (boss.isEmpty() ? "" : " (" + boss + ")") + (minTier > 0 ? " tier " + minTier + "+" : "");
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getEnum(params, "state", State.class, null) == null) {
            problems.add("Unknown slayer state " + Params.getString(params, "state", ""));
        }
    }

}
