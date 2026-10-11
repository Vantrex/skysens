package de.vantrex.skysens.client.stats;

import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/**
 * Server TPS and ping. Prefers data from SkyHanni and Odin when they are installed and falls back to
 * measuring it ourselves.
 */
public final class ServerStatsService {

    @Getter
    private static final ServerStatsService instance = new ServerStatsService();

    private final List<ServerStatsProvider> providers = List.of(
            new SkyHanniStatsProvider(),
            new OdinStatsProvider(),
            InternalStatsProvider.getInstance()
    );

    private ServerStatsService() {
    }

    public @NotNull OptionalDouble getTps() {
        for (final ServerStatsProvider provider : this.providers) {
            if (!provider.isAvailable()) {
                continue;
            }
            final OptionalDouble tps = provider.tps();
            if (tps.isPresent()) {
                ClientUtil.sendDebug("TPS from " + provider.name() + ": " + tps.getAsDouble());
                return tps;
            }
        }
        return OptionalDouble.empty();
    }

    public @NotNull CompletableFuture<OptionalInt> getPing() {
        return this.pingFrom(0);
    }

    private CompletableFuture<OptionalInt> pingFrom(int index) {
        if (index >= this.providers.size()) {
            return CompletableFuture.completedFuture(OptionalInt.empty());
        }
        final ServerStatsProvider provider = this.providers.get(index);
        if (!provider.isAvailable()) {
            return this.pingFrom(index + 1);
        }
        return provider.ping().thenCompose(ping -> {
            if (ping.isPresent()) {
                ClientUtil.sendDebug("Ping from " + provider.name() + ": " + ping.getAsInt());
                return CompletableFuture.completedFuture(ping);
            }
            return this.pingFrom(index + 1);
        });
    }

}
