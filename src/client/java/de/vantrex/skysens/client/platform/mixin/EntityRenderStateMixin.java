package de.vantrex.skysens.client.platform.mixin;

import de.vantrex.skysens.client.features.visuals.render.SkysensRenderStateExt;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements SkysensRenderStateExt {

    @Unique
    private float skysens$modelScale = 1f;

    @Unique
    private float skysens$nameTagScale = 1f;

    @Override
    public float skysens$getModelScale() {
        return this.skysens$modelScale;
    }

    @Override
    public void skysens$setModelScale(float scale) {
        this.skysens$modelScale = scale;
    }

    @Override
    public float skysens$getNameTagScale() {
        return this.skysens$nameTagScale;
    }

    @Override
    public void skysens$setNameTagScale(float scale) {
        this.skysens$nameTagScale = scale;
    }
}
