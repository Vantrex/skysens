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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** A value inside the held item's ExtraAttributes, addressed by a dotted path like {@code enchantments.ultimate_wise}. */
public final class ItemPathCondition implements ConditionType {

    public enum Op { EXISTS, EQUALS, CONTAINS }

    @Override
    public String id() {
        return "item.path";
    }

    @Override
    public String displayName() {
        return "Item attribute";
    }

    @Override
    public String group() {
        return "Items";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                ParamSpec.of("path", "Path", "Dotted path into the item's ExtraAttributes, e.g. enchantments.ultimate_wise", ParamKind.TEXT, new JsonPrimitive("")),
                new ParamSpec("op", "Check", "How to compare", ParamKind.ENUM, new JsonPrimitive(Op.EXISTS.name()),
                        () -> List.of(new Choice("EXISTS", "exists"), new Choice("EQUALS", "equals"), new Choice("CONTAINS", "contains")), null),
                new ParamSpec("value", "Value", "Value to compare with", ParamKind.TEXT, new JsonPrimitive(""),
                        null, p -> !Op.EXISTS.name().equals(Params.getString(p, "op", "EXISTS"))));
    }

    @Override
    public Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons) {
        final Optional<CompoundTag> extra = ctx.items().heldExtraAttributes();
        final String path = Params.getString(params, "path", "").trim();
        if (extra.isEmpty() || path.isEmpty()) {
            return Tri.FALSE;
        }
        Tag tag = extra.get();
        for (String part : path.split("\\.")) {
            if (!(tag instanceof CompoundTag compound) || (tag = compound.get(part)) == null) {
                return Tri.FALSE;
            }
        }
        final Op op = Params.getEnum(params, "op", Op.class, Op.EXISTS);
        final String value = Params.getString(params, "value", "");
        return Tri.of(switch (op) {
            case EXISTS -> true;
            case EQUALS -> text(tag).equalsIgnoreCase(value);
            case CONTAINS -> contains(tag, value);
        });
    }

    public static String text(Tag tag) {
        return tag.asString().orElseGet(tag::toString);
    }

    private static boolean contains(Tag tag, String value) {
        if (tag instanceof ListTag list) {
            for (Tag element : list) {
                if (text(element).equalsIgnoreCase(value)) {
                    return true;
                }
            }
            return false;
        }
        if (tag instanceof CompoundTag compound) {
            return compound.keySet().stream().anyMatch(key -> key.equalsIgnoreCase(value));
        }
        return text(tag).toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    @Override
    public String describe(JsonObject params) {
        final Op op = Params.getEnum(params, "op", Op.class, Op.EXISTS);
        final String path = Params.getString(params, "path", "?");
        return op == Op.EXISTS ? "held item has " + path
                : "held item " + path + " " + op.name().toLowerCase(Locale.ROOT) + " \"" + Params.getString(params, "value", "") + "\"";
    }

    @Override
    public void validate(JsonObject params, List<String> problems) {
        if (Params.getString(params, "path", "").isBlank()) {
            problems.add("No path set, never matches");
        }
    }

}
