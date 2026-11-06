package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ConditionTypes;
import de.vantrex.skysens.client.features.macro.condition.impl.ItemPathCondition;
import de.vantrex.skysens.client.features.macro.facts.ItemFacts;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the attributes of the item in the player's main hand for the "Use held item" button: SkyBlock id, reforge,
 * rarity, enchantments and other notable ExtraAttributes keys. Each attribute knows the rule it turns into when ticked.
 * Pets are not offered: they are covered by the Active pet condition, not by item conditions.
 */
public final class HeldItemAttributes {

    /** The kind of rule row the button sits in; decides which attributes are offered and how they become rules. */
    public enum Target { HELD, INVENTORY, PATH }

    /** One tickable attribute. {@code ruleType} / {@code params} describe the condition it becomes. */
    public record Attribute(String label, String value, String ruleType, JsonObject params, boolean defaultTicked, boolean pet) {
    }

    /** What the button can do right now: the attributes, or the reason it is disabled. */
    public record Result(List<Attribute> attributes, @Nullable String disabledReason) {
    }

    private static final Set<String> SKIPPED = Set.of("id", "modifier", "enchantments", "petInfo", "uuid", "timestamp", "originTag");
    private static final int MAX_OTHER = 12;

    private HeldItemAttributes() {
    }

    public static Result read(Target target) {
        final Minecraft mc = Minecraft.getInstance();
        final ItemStack stack = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            return new Result(List.of(), "Hold a SkyBlock item in your main hand first");
        }
        final Optional<CompoundTag> extra = HypixelExtraAttributes.getExtraAttributes(stack);
        final String id = extra.flatMap(ea -> HypixelExtraAttributes.getString(ea, "id")).orElse(null);
        if (id == null) {
            return new Result(List.of(), "The held item has no SkyBlock ID");
        }
        final CompoundTag ea = extra.get();
        final List<Attribute> out = new ArrayList<>();
        out.add(new Attribute("SkyBlock ID", id, target == Target.INVENTORY ? "item.inventory" : "item.held", with("id", id), target != Target.PATH, false));
        if (target == Target.PATH) {
            addEnchants(out, ea, target);
            addOther(out, ea);
            return new Result(out, null);
        }
        if (target == Target.HELD) {
            HypixelExtraAttributes.getString(ea, "modifier").ifPresent(
                    reforge -> out.add(new Attribute("Reforge", reforge, "item.held", with("reforge", reforge), false, false)));
            final String rarity = ItemFacts.rarityOf(stack);
            if (rarity != null) {
                final JsonObject params = new JsonObject();
                final JsonArray list = new JsonArray();
                list.add(rarity);
                params.add("rarity", list);
                out.add(new Attribute("Rarity", rarity, "item.held", params, false, false));
            }
            addEnchants(out, ea, target);
            addOther(out, ea);
        }
        return new Result(out, null);
    }

    private static JsonObject with(String key, String value) {
        final JsonObject params = new JsonObject();
        params.addProperty(key, value);
        return params;
    }

    private static void addEnchants(List<Attribute> out, CompoundTag ea, Target target) {
        final Optional<CompoundTag> enchants = ea.getCompound("enchantments");
        if (enchants.isEmpty()) {
            return;
        }
        for (String key : enchants.get().keySet()) {
            final int level = enchants.get().getInt(key).orElse(0);
            if (level <= 0) {
                continue;
            }
            if (target == Target.PATH) {
                out.add(new Attribute("Enchantment " + key, String.valueOf(level), "item.path", path("enchantments." + key, "EQUALS", String.valueOf(level)), false, false));
            } else {
                final JsonObject params = with("enchant", key);
                params.addProperty("minLevel", level);
                out.add(new Attribute("Enchantment " + key, String.valueOf(level), "item.held", params, false, false));
            }
        }
    }

    private static void addOther(List<Attribute> out, CompoundTag ea) {
        int count = 0;
        for (String key : ea.keySet()) {
            if (SKIPPED.contains(key) || count >= MAX_OTHER) {
                continue;
            }
            final Tag tag = ea.get(key);
            if (tag == null) {
                continue;
            }
            count++;
            if (tag instanceof CompoundTag || tag instanceof net.minecraft.nbt.ListTag) {
                out.add(new Attribute("Has " + key, "(exists)", "item.path", path(key, "EXISTS", ""), false, false));
            } else {
                final String value = ItemPathCondition.text(tag);
                out.add(new Attribute(key, value, "item.path", path(key, "EQUALS", value), false, false));
            }
        }
    }

    private static JsonObject path(String path, String op, String value) {
        final JsonObject params = new JsonObject();
        params.addProperty("path", path);
        params.addProperty("op", op);
        params.addProperty("value", value);
        return params;
    }

    /** A new rule {@code {id, type, negate, params}} of the type with the given params laid over its defaults. */
    public static JsonObject rule(String type, JsonObject params, java.util.function.Function<ConditionType, JsonObject> factory) {
        final ConditionType condition = ConditionTypes.get(type);
        final JsonObject rule = factory.apply(condition);
        final JsonObject target = rule.getAsJsonObject("params");
        params.entrySet().forEach(e -> target.add(e.getKey(), e.getValue().deepCopy()));
        return rule;
    }

    /** Whether a rule of the given type and params is still untouched (all defaults), so ticking can replace it. */
    public static boolean isBlank(String type, JsonObject params) {
        final ConditionType condition = ConditionTypes.get(type);
        return condition != null && ParamSpec.defaults(condition.params()).equals(params);
    }

}
