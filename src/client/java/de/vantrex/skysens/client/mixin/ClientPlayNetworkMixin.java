package de.vantrex.skysens.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.regex.Pattern;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkMixin {

    /**
     * Removes:
     *  - "Dont <Action>!"
     *  - "Don't <Action>!"
     *  - "Don’t <Action>!"
     * And if it's preceded by "and", removes that too:
     *  - " and Dont <Action>!"
     *
     * <Action> is everything up to the next '!' (non-greedy by stopping at '!').
     */
    private static final Pattern REMOVE_DONT_ACTION =
            Pattern.compile("\\s*(?:\\band\\b\\s*)?don['’]?t\\s+[^!]+!\\s*", Pattern.CASE_INSENSITIVE);

    private static Component rewrite(Component in) {
        if (in == null) return null;

        String s = in.getString();

        // Remove the "Dont <Action>!" part (and optional leading "and")
        s = REMOVE_DONT_ACTION.matcher(s).replaceAll("").trim();

        // Clean up leftover whitespace
        s = s.replaceAll("\\s{2,}", " ").trim();

        return Component.literal(s).setStyle(in.getStyle());
    }

    @ModifyArg(
            method = "setTitleText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setTitle(Lnet/minecraft/network/chat/Component;)V"
            )
    )
    private Component skysens$rewriteTitle(Component title) {
        return rewrite(title);
    }

    @ModifyArg(
            method = "setSubtitleText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setSubtitle(Lnet/minecraft/network/chat/Component;)V"
            )
    )
    private Component skysens$rewriteSubtitle(Component subtitle) {
        return rewrite(subtitle);
    }

}
