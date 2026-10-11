package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.SkysensClient;
import lombok.Getter;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Asks Hypixel (via the Mod API) whether we are in a party. */
public class PartyService {

    private static final long TIMEOUT_MS = 3000;

    @Getter
    private static final PartyService instance = new PartyService();

    private final List<CompletableFuture<Optional<Boolean>>> pending = new ArrayList<>();

    private PartyService() {
        HypixelModAPI.getInstance().createHandler(ClientboundPartyInfoPacket.class, packet -> this.complete(Optional.of(packet.isInParty())));
    }

    /**
     * Whether we are in a party. Completes on the client thread; empty if Hypixel did not answer in time.
     */
    public @NotNull CompletableFuture<Optional<Boolean>> isInParty() {
        final CompletableFuture<Optional<Boolean>> future = new CompletableFuture<>();
        final boolean sendRequest;
        synchronized (this.pending) {
            // one request answers everyone who is waiting
            sendRequest = this.pending.isEmpty();
            this.pending.add(future);
        }
        final Minecraft client = Minecraft.getInstance();
        if (sendRequest) {
            client.execute(() -> {
                if (!HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket())) {
                    this.complete(Optional.empty());
                }
            });
        }
        SkysensClient.SCHEDULER.schedule(() -> {
            synchronized (this.pending) {
                this.pending.remove(future);
            }
            client.execute(() -> future.complete(Optional.empty()));
        }, TIMEOUT_MS, TimeUnit.MILLISECONDS);
        return future;
    }

    private void complete(Optional<Boolean> result) {
        final List<CompletableFuture<Optional<Boolean>>> waiting;
        synchronized (this.pending) {
            waiting = new ArrayList<>(this.pending);
            this.pending.clear();
        }
        Minecraft.getInstance().execute(() -> waiting.forEach(future -> future.complete(result)));
    }

}
