package de.vantrex.skysens.client.platform.mixin;

import de.vantrex.skysens.client.platform.hook.ChatHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands every system chat packet to the {@code RawGameMessageListeningFeature}s before {@code ChatListener} runs
 * Fabric's message events, so before other mods can cancel or rewrite the message.
 */
@Mixin(ClientPacketListener.class)
public abstract class SystemChatPacketMixin {

    // after ensureRunningOnSameThread, so this only runs once, on the client thread
    @Inject(
            method = "handleSystemChat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void skysens$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        ChatHandler.onRawGameMessage(packet.content(), packet.overlay());
    }
}
