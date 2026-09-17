package de.vantrex.skysens.client.feature;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public interface LivingEntityFeature extends Feature {

    default void getHandSwingDuration$Head(CallbackInfoReturnable<Integer> cir) {
    }

}
