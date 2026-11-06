package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.facts.MayorFacts;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The current mayor (optionally also the minister) is a given person and / or a given perk is active. UNKNOWN until
 * the election data was fetched. Mayor and perk names are an open set (specials, new mayors), so they are free text with
 * suggestions and never a validation error.
 */
public final class MayorCondition implements ConditionType {

    private static final List<String> MAYORS = List.of("Aatrox", "Cole", "Diana", "Diaz", "Finnegan", "Foxy", "Marina", "Paul",
            "Scorpius", "Jerry", "Derpy");
    private static final List<String> PERKS = List.of(
            "Slayer XP Buff", "Pathfinder", "SLASHED Pricing",
            "Prospection", "Molten Forge", "Mining Fiesta", "Mining XP Buff",
            "Lucky!", "Mythological Ritual", "Pet XP Buff", "Sharing is Caring",
            "Volume Trading", "Shopping Spree", "Stock Exchange", "Long Term Investment",
            "Farming Simulator", "Pelt-pocalypse", "GOATed", "Blooming Business",
            "Sweet Tooth", "A Time for Giving", "Chivalrous Carnival",
            "Luck of the Sea 2.0", "Fishing XP Buff", "Fishy Treat", "Double Trouble",
            "Marauder", "EZPZ", "Benediction",
            "Bribe", "Darker Auctions",
            "Perkpocalypse", "Statspocalypse", "Jerrypocalypse",
            "TURBO MINIONS!!!", "QUAD TAXES!!!", "DOUBLE MOBS HP!!!", "MOAR SKILLZ!!!");

    private static List<Choice> merged(List<String> fixed, List<String> live) {
        final Map<String, String> out = new LinkedHashMap<>();
        live.forEach(name -> out.putIfAbsent(name.toLowerCase(Locale.ROOT), name));
        fixed.forEach(name -> out.putIfAbsent(name.toLowerCase(Locale.ROOT), name));
        final List<Choice> choices = new ArrayList<>();
        out.values().forEach(name -> choices.add(new Choice(name, name)));
        return choices;
    }

    @Override
    public String id() {
        return "mayor";
    }

    @Override
    public String displayName() {
        return "Mayor";
    }

    @Override
    public String group() {
        return "Time";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                new ParamSpec("mayor", "Mayor", "Mayor name, e.g. Diana. Empty = any", ParamKind.TEXT_CHOICES, new JsonPrimitive(""),
                        () -> merged(MAYORS, MayorFacts.liveNames()), null),
                new ParamSpec("perk", "Perk", "A perk that must be active, e.g. Mythological Ritual. Empty = any", ParamKind.TEXT_CHOICES,
                        new JsonPrimitive(""), () -> merged(PERKS, MayorFacts.livePerks()), null),
                ParamSpec.of("includeMinister", "Include the minister", "The minister counts as mayor too, and their perk as active",
                        ParamKind.BOOL, new JsonPrimitive(true)));
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final MayorFacts mayor = ctx.mayor();
        if (!mayor.known()) {
            reasons.unknown("mayor data not loaded yet");
            return Tri.UNKNOWN;
        }
        final String name = Params.getString(params, "mayor", "").trim();
        final String perk = Params.getString(params, "perk", "").trim();
        final boolean minister = Params.getBool(params, "includeMinister", true);
        if (!name.isEmpty()) {
            final boolean isMayor = name.equalsIgnoreCase(mayor.mayor());
            final boolean isMinister = minister && name.equalsIgnoreCase(mayor.minister());
            if (!isMayor && !isMinister) {
                return Tri.FALSE;
            }
        }
        if (!perk.isEmpty()) {
            return mayor.hasPerk(perk, minister);
        }
        return Tri.TRUE;
    }

    @Override
    public String describe(JsonObject params) {
        final String name = Params.getString(params, "mayor", "").trim();
        final String perk = Params.getString(params, "perk", "").trim();
        final List<String> parts = new ArrayList<>();
        if (!name.isEmpty()) {
            parts.add("mayor is " + name);
        }
        if (!perk.isEmpty()) {
            parts.add("perk " + perk + " active");
        }
        return parts.isEmpty() ? "mayor data known" : String.join(" and ", parts);
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getString(params, "mayor", "").isBlank() && Params.getString(params, "perk", "").isBlank()) {
            problems.add(WARN + "No mayor or perk set, only checks that the mayor data is loaded");
        }
    }

}
