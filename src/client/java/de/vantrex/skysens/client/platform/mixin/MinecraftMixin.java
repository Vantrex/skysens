package de.vantrex.skysens.client.platform.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.vantrex.skysens.client.core.feature.CameraFeature;
import de.vantrex.skysens.client.core.feature.FeatureService;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;cycle()Lnet/minecraft/client/CameraType;"))
    private CameraType skysens$adjustCycledCamera(CameraType next) {
        for (CameraFeature feature : FeatureService.getInstance().getFeatureRegistry().getCameraFeature()) {
            if (feature.isActive()) {
                next = feature.adjustCycledCamera(next);
            }
        }
        return next;
    }
}
