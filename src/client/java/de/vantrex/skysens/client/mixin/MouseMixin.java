package de.vantrex.skysens.client.mixin;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.MouseFeature;
import de.vantrex.skysens.client.service.FeatureService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Unique
    private final FeatureService featureService = FeatureService.getInstance();

    @Shadow
    private @Final MinecraftClient client;

    @Redirect(
            method = "updateMouse(D)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"
            )
    )
    private Object skysens$scaleSensitivity(SimpleOption<?> option) {
        Object value = option.getValue();

        if (option == client.options.getMouseSensitivity()) {
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
