package de.vantrex.skysens.client.features.macro.condition.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ReasonSink;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.facts.ItemFacts;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.facts.PetFacts;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.skyblock.pet.Pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The active (summoned) pet, optionally filtered by type, rarity, minimum level and held pet item. UNKNOWN until the pet
 * of the profile was restored (then "no pet" is UNKNOWN too). The held item is only known for pets read from an item
 * (the /pets menu); a pet seen only through the tab list or chat has none recorded, so a held item filter is UNKNOWN
 * for it rather than FALSE.
 */
public final class ActivePetCondition implements ConditionType {

    @Override
    public String id() {
        return "pet.active";
    }

    @Override
    public String displayName() {
        return "Active pet";
    }

    @Override
    public String group() {
        return "Pets";
    }

    @Override
    public List<ParamSpec> params() {
        final List<ParamSpec> out = new ArrayList<>(PetFilter.specs());
        out.add(ParamSpec.of("heldItem", "Held pet item", "Id of the pet item, * is a wildcard (e.g. PET_ITEM_TIER_BOOST). Empty = any",
                ParamKind.TEXT, new JsonPrimitive("")));
        return out;
    }

    @Override
    public boolean trackerBacked() {
        return true;
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final PetFacts facts = ctx.pet();
        if (facts.known() != Tri.TRUE) {
            reasons.unknown("pet state not known yet");
            return Tri.UNKNOWN;
        }
        final Optional<Pet> active = facts.active();
        if (active.isEmpty()) {
            return Tri.FALSE;
        }
        final Pet pet = active.get();
        final Tri filter = PetFilter.test(params, pet, reasons);
        if (filter == Tri.FALSE) {
            return Tri.FALSE;
        }
        final String held = Params.getString(params, "heldItem", "").trim();
        if (!held.isEmpty()) {
            if (pet.heldItem() != null) {
                if (!ItemFacts.matchesGlob(pet.heldItem(), held)) {
                    return Tri.FALSE;
                }
            } else {
                reasons.unknown("held item of the active pet not known (open /pets once)");
                return Tri.UNKNOWN;
            }
        }
        return filter;
    }

    @Override
    public String describe(JsonObject params) {
        final String held = Params.getString(params, "heldItem", "").trim();
        final String filter = PetFilter.describe(params);
        final List<String> parts = new ArrayList<>();
        if (!filter.isEmpty()) {
            parts.add(filter);
        }
        if (!held.isEmpty()) {
            parts.add("holding " + held);
        }
        return parts.isEmpty() ? "has an active pet" : "active pet " + String.join(", ", parts);
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        PetFilter.validate(params, problems);
    }

}
