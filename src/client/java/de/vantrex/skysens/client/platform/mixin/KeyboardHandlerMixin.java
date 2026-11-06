package de.vantrex.skysens.client.platform.mixin;

import de.vantrex.skysens.client.core.feature.FeatureService;
import de.vantrex.skysens.client.core.feature.RawInputFeature;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Unique
    private final FeatureService featureService = FeatureService.getInstance();

    @Shadow
    private @Final Minecraft minecraft;

    /** Raw key events before screens and key mappings; cancelling swallows the key press. */
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void skysens$keyPress$Head(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window != minecraft.getWindow().handle()) {
            return;
        }
        for (RawInputFeature feature : featureService.getFeatureRegistry().getRawInputFeature()) {
            if (!feature.isActive()) {
                continue;
            }
            feature.keyPress$Head(event, action, ci);
            if (ci.isCancelled()) {
                return;
            }
        }
    }

}
