package de.vantrex.skysens.client.mixin;

import de.vantrex.skysens.client.feature.dungeon.DungeonRunDetector;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Messages other mods add to the chat directly (e.g. Odin's run summary) fire no game message event. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "addClientSystemMessage", at = @At("HEAD"))
    private void skysens$onClientSystemMessage(Component message, CallbackInfo ci) {
        DungeonRunDetector.getInstance().onClientSystemMessage(message);
    }
}
