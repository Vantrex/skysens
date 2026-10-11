package de.vantrex.skysens.client.stats;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/**
 * Reads TPS from SkyHanni's TpsCounter. For ping, SkyHanni (with its "Ping API" toggle on) keeps the vanilla
 * debug ping log filled and averages it, so we average the same log while that toggle is on.
 */
final class SkyHanniStatsProvider implements ServerStatsProvider {

    private static final int PING_SAMPLES = 20;

    private final boolean loaded = FabricLoader.getInstance().isModLoaded("skyhanni");
    private final KotlinObjectAccessor tpsCounter = new KotlinObjectAccessor("at.hannibal2.skyhanni.features.misc.TpsCounter");
    private final KotlinObjectAccessor currentPing = new KotlinObjectAccessor("at.hannibal2.skyhanni.features.misc.CurrentPing");

    @Override
    public @NotNull String name() {
        return "SkyHanni";
    }

    @Override
    public boolean isAvailable() {
        return this.loaded;
    }

    @Override
    public @NotNull OptionalDouble tps() {
        // null while SkyHanni is still calculating after a world switch
        if (this.tpsCounter.call("getTps") instanceof Double tps && tps > 0) {
            return OptionalDouble.of(tps);
        }
        return OptionalDouble.empty();
    }

    @Override
    public @NotNull CompletableFuture<OptionalInt> ping() {
        if (!Boolean.TRUE.equals(this.currentPing.call("isEnabled"))) {
            return CompletableFuture.completedFuture(OptionalInt.empty());
        }
        final LocalSampleLogger log = Minecraft.getInstance().getDebugOverlay().getPingLogger();
        final int size = Math.min(log.size(), PING_SAMPLES);
        if (size == 0) {
            return CompletableFuture.completedFuture(OptionalInt.empty());
        }
        long total = 0;
        for (int i = 0; i < size; i++) {
            total += log.get(i);
        }
        return CompletableFuture.completedFuture(OptionalInt.of((int) (total / size)));
    }

}
