package de.vantrex.skysens.client.mixin;

import de.vantrex.skysens.client.feature.LivingEntityFeature;
import de.vantrex.skysens.client.service.FeatureService;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin  {

    @Unique
    private final FeatureService featureService = FeatureService.getInstance();

    @Inject(method = {"getHandSwingDuration"}, at = {@At("HEAD")}, cancellable = true)
    private void getHandSwingDuration$Head(CallbackInfoReturnable<Integer> cir) {
        for (LivingEntityFeature livingEntityFeature : featureService.getFeatureRegistry().getLivingEntityFeature()) {
            if (livingEntityFeature.isActive()) {
                livingEntityFeature.getHandSwingDuration$Head(cir);
            }
        }
    }
}
