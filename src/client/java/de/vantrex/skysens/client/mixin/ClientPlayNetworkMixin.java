package de.vantrex.skysens.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
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

    private static Text rewrite(Text in) {
        if (in == null) return null;

        String s = in.getString();

        // Remove the "Dont <Action>!" part (and optional leading "and")
        s = REMOVE_DONT_ACTION.matcher(s).replaceAll("").trim();

        // Clean up leftover whitespace
        s = s.replaceAll("\\s{2,}", " ").trim();

        return Text.literal(s).setStyle(in.getStyle());
    }

    @ModifyArg(
            method = "onTitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;setTitle(Lnet/minecraft/text/Text;)V"
            )
    )
    private Text skysens$rewriteTitle(Text title) {
        return rewrite(title);
    }

    @ModifyArg(
            method = "onSubtitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;setSubtitle(Lnet/minecraft/text/Text;)V"
            )
    )
    private Text skysens$rewriteSubtitle(Text subtitle) {
        return rewrite(subtitle);
    }

}
