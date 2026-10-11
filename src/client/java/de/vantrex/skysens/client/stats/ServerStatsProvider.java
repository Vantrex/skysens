package de.vantrex.skysens.client.stats;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/** A source of server statistics. Empty results make {@link ServerStatsService} try the next provider. */
public interface ServerStatsProvider {

    /** Display name, used for debug output. */
    @NotNull String name();

    /** Whether this provider can be used at all, e.g. whether the backing mod is installed. */
    boolean isAvailable();

    @NotNull OptionalDouble tps();

    /** Round trip time in milliseconds. May complete later if a measurement is needed, but always on the client thread. */
    @NotNull CompletableFuture<OptionalInt> ping();

}
