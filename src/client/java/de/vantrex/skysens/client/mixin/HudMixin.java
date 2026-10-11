package de.vantrex.skysens.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.vantrex.skysens.client.feature.CameraFeature;
import de.vantrex.skysens.client.service.FeatureService;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
public abstract class HudMixin {

    @ModifyExpressionValue(method = "extractCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean skysens$thirdPersonCrosshair(boolean firstPerson) {
        if (firstPerson) {
            return true;
        }
        for (CameraFeature feature : FeatureService.getInstance().getFeatureRegistry().getCameraFeature()) {
            if (feature.isActive() && feature.forceCrosshair()) {
                return true;
            }
        }
        return false;
    }
}
