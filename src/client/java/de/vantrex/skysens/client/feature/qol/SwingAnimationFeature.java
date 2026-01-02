package de.vantrex.skysens.client.feature.qol;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.qol.sub.SwingAnimationSubCategory;
import de.vantrex.skysens.client.feature.LivingEntityFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SkySensFeature
public class SwingAnimationFeature implements LivingEntityFeature {

    private final SwingAnimationSubCategory swingConfig = SkysensConfig.getInstance().qolCategory.swingAnimation;
    private final static int BASE_SPEED = 10;

    @Override
    public void getHandSwingDuration$Head(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.calculateSwingSpeed());
    }

    private int calculateSwingSpeed() {
        int adjustedSpeed = this.swingConfig.swingSpeed;
        return BASE_SPEED - adjustedSpeed;
    }

    @Override
    public boolean isActive() {
        return this.swingConfig.enabled;
    }
}
