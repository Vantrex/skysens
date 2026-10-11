package de.vantrex.skysens.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.vantrex.skysens.client.render.EntityScaleContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

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
}
