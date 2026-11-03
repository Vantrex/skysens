package de.vantrex.skysens.client.core.feature;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public interface LivingEntityFeature extends Feature {

    default void getHandSwingDuration$Head(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
    }

    default void getAttackAnim$Head(LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir) {
    }

}
