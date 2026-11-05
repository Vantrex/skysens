package de.vantrex.skysens.client.features.macro.action.placeholder;

import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The known {{ paths }}. A resolver returns null when the value is not available. Also feeds editor autocomplete. */
public final class PlaceholderRegistry {

    private static final Map<String, Function<MacroContext, @Nullable String>> RESOLVERS = new LinkedHashMap<>();

    static {
        RESOLVERS.put("player", ctx -> Minecraft.getInstance().getUser().getName());
        RESOLVERS.put("island", ctx -> {
            final var location = LocationService.getInstance().getCurrentLocation();
            return location == null ? null : location.getDisplayName();
        });
        RESOLVERS.put("zone", ctx -> {
            final var zone = LocationService.getInstance().getCurrentZone();
            return zone == null ? null : zone.getZoneEnum().getDisplayName();
        });
        RESOLVERS.put("heldItem.id", ctx -> ctx.heldExtraAttributes()
                .flatMap(ea -> HypixelExtraAttributes.getString(ea, "id")).orElse(null));
        RESOLVERS.put("heldItem.name", ctx -> ctx.heldItem().isEmpty() ? null : ctx.heldItem().getHoverName().getString());
        RESOLVERS.put("heldItem.reforge", ctx -> ctx.heldExtraAttributes()
                .flatMap(ea -> HypixelExtraAttributes.getString(ea, "modifier")).orElse(null));
        RESOLVERS.put("party.leader", ctx -> ctx.party().leader());
        RESOLVERS.put("party.size", ctx -> {
            final Integer size = ctx.party().size();
            return size == null ? null : String.valueOf(size);
        });
        RESOLVERS.put("party.members", ctx -> {
            final var names = ctx.party().memberNames();
            return names.isEmpty() ? null : String.join(", ", names);
        });
        RESOLVERS.put("slayer.boss", ctx -> ctx.slayer().bossName());
        RESOLVERS.put("slayer.tier", ctx -> ctx.slayer().tier() > 0 ? String.valueOf(ctx.slayer().tier()) : null);
        RESOLVERS.put("container.title", ctx -> ctx.screen().containerTitle());
    }

    private PlaceholderRegistry() {
    }

    public static boolean isKnown(String path) {
        return RESOLVERS.containsKey(path);
    }

    public static List<String> paths() {
        return List.copyOf(RESOLVERS.keySet());
    }

    /** The value, or null when the path is unknown or has no value right now. */
    public static @Nullable String resolve(String path, MacroContext ctx) {
        final Function<MacroContext, String> resolver = RESOLVERS.get(path);
        return resolver == null ? null : resolver.apply(ctx);
    }

}
