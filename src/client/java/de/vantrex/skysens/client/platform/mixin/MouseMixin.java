package de.vantrex.skysens.client.platform.mixin;

import de.vantrex.skysens.client.core.feature.MouseFeature;
import de.vantrex.skysens.client.core.feature.RawInputFeature;
import de.vantrex.skysens.client.core.feature.FeatureService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {

    @Unique
    private final FeatureService featureService = FeatureService.getInstance();

    @Shadow
    private @Final Minecraft minecraft;

    @Redirect(
            method = "turnPlayer(D)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
            )
    )
    private Object skysens$scaleSensitivity(OptionInstance<?> option) {
        Object value = option.get();

        if (option == minecraft.options.sensitivity()) {
            for (MouseFeature mouseFeature : featureService.getFeatureRegistry().getMouseFeature()) {
                if (!mouseFeature.isActive()) {
                    continue;
                }
                Double scale = mouseFeature.getMouseSensitivityScale();
                if (scale == null) {
                    continue;
                }
                return scale;
            }
        }

        return value;
    }

    /** Raw mouse buttons before screens and key mappings see them; cancelling swallows the click. */
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void skysens$onButton$Head(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (window != minecraft.getWindow().handle()) {
            return;
        }
        for (RawInputFeature feature : featureService.getFeatureRegistry().getRawInputFeature()) {
            if (!feature.isActive()) {
                continue;
            }
            feature.mouseButton$Head(info, action, ci);
            if (ci.isCancelled()) {
                return;
            }
        }
    }

}
