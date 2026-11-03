package de.vantrex.skysens.client.platform.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.vantrex.skysens.client.features.visuals.render.EntityScaleHelper;
import de.vantrex.skysens.client.features.visuals.render.SkysensRenderStateExt;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "finalizeRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)V", at = @At("HEAD"))
    private void skysens$applyScale(Entity entity, EntityRenderState state, CallbackInfo ci) {
        EntityScaleHelper.apply(entity, state);
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void skysens$scaleNameTag(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, int light, CallbackInfo ci) {
        final float scale = ((SkysensRenderStateExt) state).skysens$getNameTagScale();
        if (scale != 1f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
