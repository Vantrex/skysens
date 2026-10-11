package de.vantrex.skysens.client.stats;

import de.vantrex.skysens.client.SkysensClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Measures TPS and ping without other mods.
 * <ul>
 *     <li>TPS: the server sends a time update every 20 ticks, so 20 ticks divided by the time between
 *     two updates is the TPS. Averaged over the last {@value #TPS_SAMPLES} updates.</li>
 *     <li>Ping: vanilla only pings while the F3 network chart is open, so we send a ping request on
 *     demand and time the pong. Pongs to requests of vanilla or other mods are used too.</li>
 * </ul>
 * Fed by {@code ServerStatsMixin}.
 */
public final class InternalStatsProvider implements ServerStatsProvider {

    private static final int TPS_SAMPLES = 10;
    private static final int MIN_TPS_SAMPLES = 3;
    private static final long PING_MAX_AGE_MS = 5000;
    private static final long PING_TIMEOUT_MS = 3000;

    private static final InternalStatsProvider INSTANCE = new InternalStatsProvider();

    // TPS state, only touched on the client thread
    private final double[] tpsSamples = new double[TPS_SAMPLES];
    private int tpsIndex;
    private int tpsCount;
    private long lastTimePacketNanos;
    private ClientLevel lastLevel;

    // ping state, pongs arrive on the network thread
    private final List<CompletableFuture<OptionalInt>> pendingPings = new ArrayList<>();
    private volatile int lastPing = -1;
    private volatile long lastPingAt;

    private InternalStatsProvider() {
    }

    public static InternalStatsProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public @NotNull String name() {
        return "SkySens";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /** Called on the client thread for every time update packet. */
    public void onTimeUpdate() {
        final long now = System.nanoTime();
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != this.lastLevel) {
            // new world: the gap since the last packet says nothing about the server
            this.lastLevel = level;
            this.tpsCount = 0;
            this.tpsIndex = 0;
            this.lastTimePacketNanos = now;
            return;
        }
        final double elapsedMs = (now - this.lastTimePacketNanos) / 1_000_000.0;
        this.lastTimePacketNanos = now;
        if (elapsedMs <= 0) {
            return;
        }
        this.tpsSamples[this.tpsIndex] = Math.min(20.0, 20_000.0 / elapsedMs);
        this.tpsIndex = (this.tpsIndex + 1) % TPS_SAMPLES;
        this.tpsCount = Math.min(this.tpsCount + 1, TPS_SAMPLES);
    }

    /** Called on the network thread for every pong. {@code requestTime} is the {@link Util#getMillis()} of the request. */
    public void onPong(long requestTime) {
        final int ping = (int) Math.max(0, Util.getMillis() - requestTime);
        this.lastPing = ping;
        this.lastPingAt = System.currentTimeMillis();
        final List<CompletableFuture<OptionalInt>> waiting;
        synchronized (this.pendingPings) {
            waiting = new ArrayList<>(this.pendingPings);
            this.pendingPings.clear();
        }
        // complete on the client thread, so callers can safely touch the game (e.g. send chat)
        Minecraft.getInstance().execute(() -> waiting.forEach(future -> future.complete(OptionalInt.of(ping))));
    }

    @Override
    public @NotNull OptionalDouble tps() {
        if (this.tpsCount < MIN_TPS_SAMPLES) {
            return OptionalDouble.empty();
        }
        double total = 0;
        for (int i = 0; i < this.tpsCount; i++) {
            total += this.tpsSamples[i];
        }
        return OptionalDouble.of(total / this.tpsCount);
    }

    @Override
    public @NotNull CompletableFuture<OptionalInt> ping() {
        if (this.lastPing >= 0 && System.currentTimeMillis() - this.lastPingAt <= PING_MAX_AGE_MS) {
            return CompletableFuture.completedFuture(OptionalInt.of(this.lastPing));
        }
        final CompletableFuture<OptionalInt> future = new CompletableFuture<>();
        synchronized (this.pendingPings) {
            this.pendingPings.add(future);
        }
        final Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            final ClientPacketListener connection = client.getConnection();
            if (connection == null) {
                future.complete(OptionalInt.empty());
                return;
            }
            connection.send(new ServerboundPingRequestPacket(Util.getMillis()));
        });
        SkysensClient.SCHEDULER.schedule(() -> {
            synchronized (this.pendingPings) {
                this.pendingPings.remove(future);
            }
            client.execute(() -> future.complete(OptionalInt.empty()));
        }, PING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        return future;
    }

}
