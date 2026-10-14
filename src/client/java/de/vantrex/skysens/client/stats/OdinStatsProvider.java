package de.vantrex.skysens.client.stats;

import de.vantrex.skysens.client.util.KotlinObjectAccessor;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/** Reads TPS and ping from Odin's ServerUtils. Odin pings the server continuously while it is installed. */
final class OdinStatsProvider implements ServerStatsProvider {

    private final boolean loaded = FabricLoader.getInstance().isModLoaded("odin");
    private final KotlinObjectAccessor serverUtils = new KotlinObjectAccessor("com.odtheking.odin.utils.ServerUtils");

    @Override
    public @NotNull String name() {
        return "Odin";
    }

    @Override
    public boolean isAvailable() {
        return this.loaded;
    }

    @Override
    public @NotNull OptionalDouble tps() {
        if (this.serverUtils.call("getAverageTps") instanceof Float tps && tps > 0) {
            return OptionalDouble.of(tps);
        }
        return OptionalDouble.empty();
    }

    @Override
    public @NotNull CompletableFuture<OptionalInt> ping() {
        // 0 means Odin has not received a pong yet
        if (this.serverUtils.call("getCurrentPing") instanceof Integer ping && ping > 0) {
            return CompletableFuture.completedFuture(OptionalInt.of(ping));
        }
        return CompletableFuture.completedFuture(OptionalInt.empty());
    }

}
