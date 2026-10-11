package de.vantrex.skysens.client.mixin;

import de.vantrex.skysens.client.feature.LivingEntityFeature;
import de.vantrex.skysens.client.service.FeatureService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = {"getCurrentSwingDuration"}, at = {@At("HEAD")}, cancellable = true)
    private void getHandSwingDuration$Head(CallbackInfoReturnable<Integer> cir) {
        for (LivingEntityFeature livingEntityFeature : FeatureService.getInstance().getFeatureRegistry().getLivingEntityFeature()) {
            if (livingEntityFeature.isActive()) {
                livingEntityFeature.getHandSwingDuration$Head((LivingEntity) (Object) this, cir);
                if (cir.isCancelled()) {
                    return;
                }
            }
        }
    }

    @Inject(method = "getAttackAnim(F)F", at = @At("HEAD"), cancellable = true)
    private void getAttackAnim$Head(float partialTick, CallbackInfoReturnable<Float> cir) {
        for (LivingEntityFeature livingEntityFeature : FeatureService.getInstance().getFeatureRegistry().getLivingEntityFeature()) {
            if (livingEntityFeature.isActive()) {
                livingEntityFeature.getAttackAnim$Head((LivingEntity) (Object) this, partialTick, cir);
                if (cir.isCancelled()) {
                    return;
                }
            }
        }
    }
}
