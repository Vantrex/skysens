package de.vantrex.skysens.client.features.macro.condition;

import de.vantrex.skysens.client.features.macro.condition.impl.ActivityCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.ContainerCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.HeldItemCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.InventoryItemCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.IslandCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.ItemPathCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.PartyCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.SlayerCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.SneakingCondition;
import de.vantrex.skysens.client.features.macro.condition.impl.ZoneCondition;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit registry of condition types. Add a register(...) call per type. */
public final class ConditionTypes {

    private static final Map<String, ConditionType> TYPES = new LinkedHashMap<>();

    static {
        register(new IslandCondition());
        register(new ZoneCondition());
        register(new ActivityCondition());
        register(new SlayerCondition());
        register(new PartyCondition());
        register(new HeldItemCondition());
        register(new InventoryItemCondition());
        register(new ItemPathCondition());
        register(new ContainerCondition());
        register(new SneakingCondition());
    }

    private ConditionTypes() {
    }

    private static void register(ConditionType type) {
        if (TYPES.put(type.id(), type) != null) {
            throw new IllegalStateException("Duplicate condition type " + type.id());
        }
    }

    public static Collection<ConditionType> all() {
        return TYPES.values();
    }

    public static @Nullable ConditionType get(String id) {
        return TYPES.get(id);
    }

}
