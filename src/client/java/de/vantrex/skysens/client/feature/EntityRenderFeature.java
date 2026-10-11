package de.vantrex.skysens.client.feature;

import de.vantrex.skysens.client.util.EntityClassifier;
import net.minecraft.world.entity.Entity;

public interface EntityRenderFeature extends Feature {

    default float getModelScale(Entity entity, EntityClassifier.Kind kind) {
        return 1f;
    }

    default boolean scaleNameTag(Entity entity, EntityClassifier.Kind kind) {
        return false;
    }

    default boolean shouldHide(Entity entity, EntityClassifier.Kind kind) {
        return false;
    }

}
