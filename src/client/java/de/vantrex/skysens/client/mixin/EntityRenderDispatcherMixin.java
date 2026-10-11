package de.vantrex.skysens.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.vantrex.skysens.client.feature.EntityRenderFeature;
import de.vantrex.skysens.client.render.EntityScaleContext;
import de.vantrex.skysens.client.service.FeatureService;
import de.vantrex.skysens.client.util.EntityClassifier;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @WrapMethod(method = "extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
    private EntityRenderState skysens$markWorldExtract(Entity entity, float partialTick, Operation<EntityRenderState> original) {
        EntityScaleContext.enter();
        try {
            return original.call(entity, partialTick);
        } finally {
            EntityScaleContext.exit();
        }
    }

    @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
    private void skysens$hide(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EntityClassifier.Kind kind = null;
        for (EntityRenderFeature feature : FeatureService.getInstance().getFeatureRegistry().getEntityRenderFeature()) {
            if (!feature.isActive()) {
                continue;
            }
            if (kind == null) {
                kind = EntityClassifier.classify(entity);
            }
            if (feature.shouldHide(entity, kind)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
