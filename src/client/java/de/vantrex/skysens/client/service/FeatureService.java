package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.feature.bazaar.BazaarDisableClickFeature;
import de.vantrex.skysens.client.feature.bazaar.BazaarHighlightFeature;
import de.vantrex.skysens.client.feature.qol.SwingAnimationFeature;
import lombok.Getter;

@Getter
public class FeatureService {

    @Getter
    private static final FeatureService instance = new FeatureService();

    private final FeatureRegistry featureRegistry = new FeatureRegistry();

    private FeatureService() {
        this.register();
    }

    public void register() {
        this.featureRegistry.register(new BazaarHighlightFeature());
        this.featureRegistry.register(new BazaarDisableClickFeature());
        this.featureRegistry.register(new SwingAnimationFeature());
    }


}
