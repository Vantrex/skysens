package de.vantrex.skysens.client.features.visuals.render;

import de.vantrex.skysens.client.core.feature.EntityRenderFeature;
import de.vantrex.skysens.client.core.feature.FeatureService;
import de.vantrex.skysens.client.skyblock.entity.EntityClassifier;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

@UtilityClass
public class EntityScaleHelper {

    public void apply(Entity entity, EntityRenderState state) {
        final SkysensRenderStateExt ext = (SkysensRenderStateExt) state;
        if (!EntityScaleContext.isWorldExtract()) {
            ext.skysens$setModelScale(1f);
            ext.skysens$setNameTagScale(1f);
            return;
        }

        final EntityClassifier.Kind kind = EntityClassifier.classify(entity);
        float scale = 1f;
        boolean nameTag = false;
        for (EntityRenderFeature feature : FeatureService.getInstance().getFeatureRegistry().getEntityRenderFeature()) {
            if (!feature.isActive()) {
                continue;
            }
            scale *= feature.getModelScale(entity, kind);
            nameTag |= feature.scaleNameTag(entity, kind);
        }

        ext.skysens$setModelScale(scale);
        if (state instanceof LivingEntityRenderState living) {
            living.scale *= scale;
        }
        ext.skysens$setNameTagScale(nameTag ? scale : 1f);
    }

}
