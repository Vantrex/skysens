package de.vantrex.skysens.client.feature;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class FeatureRegistry {

    private final Set<HandledScreenFeature> handledScreenFeature = new HashSet<>();
    private final Set<LivingEntityFeature> livingEntityFeature = new HashSet<>();

    public void register(Feature feature) {
        if (feature instanceof HandledScreenFeature handledScreenFeature) {
            this.handledScreenFeature.add(handledScreenFeature);
        } else if (feature instanceof LivingEntityFeature livingEntityFeature) {
            this.livingEntityFeature.add(livingEntityFeature);
        } else {
            throw new IllegalArgumentException("Unknown feature type: " + feature.getClass());
        }
    }

}
