package de.vantrex.skysens.client.price;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Uses SkyHanni's item name resolver and price lookup (bazaar, then lowest BIN). SkyHanni's NeuInternalName is a
 * Kotlin value class, so on the JVM it is a plain String and the methods using it have mangled names
 * ({@code getPriceOrNull-abc123}); we look them up by name prefix and parameter types.
 */
final class SkyHanniPriceProvider implements ItemPriceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");

    private final boolean loaded = FabricLoader.getInstance().isModLoaded("skyhanni");
    private boolean resolved;
    private boolean broken;
    private Object nameCompanion;
    private Method fromItemName;
    private Object priceUtils;
    private Method getPrice;
    private Object priceSource;

    @Override
    public @NotNull String name() {
        return "SkyHanni";
    }

    @Override
    public boolean isAvailable() {
        return this.loaded && !this.broken;
    }

    @Override
    public synchronized @NotNull OptionalDouble price(@NotNull String itemName) {
        if (!this.isAvailable() || !this.resolve()) {
            return OptionalDouble.empty();
        }
        try {
            if (!(this.fromItemName.invoke(this.nameCompanion, itemName) instanceof String internalName)) {
                return OptionalDouble.empty();
            }
            if (this.getPrice.invoke(this.priceUtils, internalName, this.priceSource, List.of()) instanceof Double price && price > 0) {
                return OptionalDouble.of(price);
            }
            return OptionalDouble.empty();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            this.fail(e);
            return OptionalDouble.empty();
        }
    }

    private boolean resolve() {
        if (this.resolved) {
            return true;
        }
        try {
            final Class<?> nameClass = Class.forName("at.hannibal2.skyhanni.utils.NeuInternalName");
            this.nameCompanion = nameClass.getField("Companion").get(null);
            this.fromItemName = findMethod(this.nameCompanion.getClass(), "fromItemNameOrNull", String.class);

            final Class<?> priceUtilsClass = Class.forName("at.hannibal2.skyhanni.utils.ItemPriceUtils");
            final Class<?> priceSourceClass = Class.forName("at.hannibal2.skyhanni.utils.ItemPriceSource");
            this.priceUtils = priceUtilsClass.getField("INSTANCE").get(null);
            this.getPrice = findMethod(priceUtilsClass, "getPriceOrNull", String.class, priceSourceClass, List.class);
            // what the drop would sell for right now
            this.priceSource = priceSourceClass.getField("BAZAAR_INSTANT_SELL").get(null);

            if (this.fromItemName == null || this.getPrice == null) {
                throw new NoSuchMethodException("SkyHanni price methods not found");
            }
            this.resolved = true;
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            this.fail(e);
            return false;
        }
    }

    private static @Nullable Method findMethod(Class<?> owner, String namePrefix, Class<?>... parameterTypes) {
        for (final Method method : owner.getMethods()) {
            if ((method.getName().equals(namePrefix) || method.getName().startsWith(namePrefix + "-"))
                    && java.util.Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                return method;
            }
        }
        return null;
    }

    private void fail(Throwable e) {
        this.broken = true;
        LOGGER.warn("Could not use SkyHanni's item prices, falling back to SkySens' own price data", e);
    }

}
