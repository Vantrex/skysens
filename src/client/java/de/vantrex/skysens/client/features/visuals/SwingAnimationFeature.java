package de.vantrex.skysens.client.features.visuals;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.visuals.config.SwingAnimationAccordion;
import de.vantrex.skysens.client.core.feature.LivingEntityFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SkySensFeature
public class SwingAnimationFeature implements LivingEntityFeature {

    private final SwingAnimationAccordion swingConfig = SkysensConfig.getInstance().visualsCategory.swingAnimation;
    private final static int BASE_SPEED = 10;

    @Override
    public void getHandSwingDuration$Head(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (!this.swingConfig.enabled || !this.appliesTo(entity)) {
            return;
        }
        int duration = BASE_SPEED - this.swingConfig.swingSpeed;
        if (!this.swingConfig.ignoreMiningEffects) {
            if (MobEffectUtil.hasDigSpeed(entity)) {
                duration -= 1 + MobEffectUtil.getDigSpeedAmplification(entity);
            } else if (entity.hasEffect(MobEffects.MINING_FATIGUE)) {
                duration += (1 + entity.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2;
            }
        }
        cir.setReturnValue(Math.max(1, duration));
    }

    @Override
    public void getAttackAnim$Head(LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (this.swingConfig.disableSwingAnimation && this.appliesTo(entity)) {
            cir.setReturnValue(0f);
        }
    }

    private boolean appliesTo(LivingEntity entity) {
        return entity.level().isClientSide()
                && (this.swingConfig.applyToAllEntities || entity == Minecraft.getInstance().player);
    }

    @Override
    public boolean isActive() {
        return this.swingConfig.enabled || this.swingConfig.disableSwingAnimation;
    }
}
