package de.vantrex.skysens.client.features.macro.condition.impl;

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

import java.util.List;
import java.util.Locale;

/** Party state: in a party, leader, size or a given member. */
public final class PartyCondition implements ConditionType {

    public enum Check { IN_PARTY, IS_LEADER, SIZE, MEMBER_PRESENT }

    public enum Op { GE, LE, EQ }

    @Override
    public String id() {
        return "party";
    }

    @Override
    public String displayName() {
        return "Party";
    }

    @Override
    public String group() {
        return "Party";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("check", "Check", "What to check", ParamKind.ENUM, new JsonPrimitive(Check.IN_PARTY.name()),
                        () -> List.of(new Choice("IN_PARTY", "In a party"), new Choice("IS_LEADER", "Is party leader"),
                                new Choice("SIZE", "Party size"), new Choice("MEMBER_PRESENT", "Member present")), null),
                new ParamSpec("op", "Compare", "How to compare the size", ParamKind.ENUM, new JsonPrimitive(Op.GE.name()),
                        () -> List.of(new Choice("GE", "at least"), new Choice("LE", "at most"), new Choice("EQ", "exactly")),
                        p -> Check.SIZE.name().equals(Params.getString(p, "check", ""))),
                new ParamSpec("n", "Size", "Party size including you", ParamKind.INT, new JsonPrimitive(2),
                        null, p -> Check.SIZE.name().equals(Params.getString(p, "check", ""))),
                new ParamSpec("name", "Player", "Member name", ParamKind.TEXT, new JsonPrimitive(""),
                        null, p -> Check.MEMBER_PRESENT.name().equals(Params.getString(p, "check", ""))));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final Check check = Params.getEnum(params, "check", Check.class, Check.IN_PARTY);
        final Tri result;
        switch (check) {
            case IN_PARTY -> result = ctx.party().inParty();
            case IS_LEADER -> result = ctx.party().isLeader();
            case SIZE -> {
                final Integer size = ctx.party().size();
                final int n = Params.getInt(params, "n", 2);
                if (size == null) {
                    result = Tri.UNKNOWN;
                } else {
                    result = Tri.of(switch (Params.getEnum(params, "op", Op.class, Op.GE)) {
                        case GE -> size >= n;
                        case LE -> size <= n;
                        case EQ -> size == n;
                    });
                }
            }
            case MEMBER_PRESENT -> result = ctx.party().hasMember(Params.getString(params, "name", ""));
            default -> result = Tri.UNKNOWN;
        }
        if (result == Tri.UNKNOWN) {
            reasons.unknown("party state not known yet");
        }
        return result;
    }

    @Override
    public String describe(JsonObject params) {
        final Check check = Params.getEnum(params, "check", Check.class, Check.IN_PARTY);
        return switch (check) {
            case IN_PARTY -> "in a party";
            case IS_LEADER -> "is party leader";
            case SIZE -> "party size " + Params.getString(params, "op", "GE").toLowerCase(Locale.ROOT) + " " + Params.getInt(params, "n", 2);
            case MEMBER_PRESENT -> "party has " + Params.getString(params, "name", "?");
        };
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getEnum(params, "check", Check.class, null) == null) {
            problems.add("Unknown party check " + Params.getString(params, "check", ""));
        } else if (Params.getEnum(params, "check", Check.class, null) == Check.MEMBER_PRESENT
                && Params.getString(params, "name", "").isBlank()) {
            problems.add("No player name set");
        }
    }

}
