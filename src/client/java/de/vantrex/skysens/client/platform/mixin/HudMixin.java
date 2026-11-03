package de.vantrex.skysens.client.platform.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.CameraFeature;
import de.vantrex.skysens.client.core.feature.FeatureService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
    private static void skysens$hideArmorBar(GuiGraphicsExtractor graphics, Player player, int a, int b, int c, int d, CallbackInfo ci) {
        if (SkysensConfig.getInstance().qolCategory.hideArmorBar) {
            ci.cancel();
        }
    }
}
