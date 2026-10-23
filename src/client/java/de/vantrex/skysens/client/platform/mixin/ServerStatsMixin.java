package de.vantrex.skysens.client.platform.mixin;

import de.vantrex.skysens.client.stats.InternalStatsProvider;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds {@link InternalStatsProvider} with time update and pong packets. */
@Mixin(ClientPacketListener.class)
public abstract class ServerStatsMixin {

    // after ensureRunningOnSameThread, so this only runs once, on the client thread
    @Inject(
            method = "handleSetTime",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void skysens$onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        InternalStatsProvider.getInstance().onTimeUpdate();
    }

    // runs on the network thread
    @Inject(method = "handlePongResponse", at = @At("HEAD"))
    private void skysens$onPong(ClientboundPongResponsePacket packet, CallbackInfo ci) {
        InternalStatsProvider.getInstance().onPong(packet.time());
    }

}
