package de.vantrex.skysens.client.mixin;

import de.vantrex.skysens.client.feature.MouseFeature;
import de.vantrex.skysens.client.service.FeatureService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

}
