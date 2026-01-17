package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.feature.bazaar.BazaarDisableClickFeature;
import de.vantrex.skysens.client.feature.bazaar.BazaarHighlightFeature;
import de.vantrex.skysens.client.feature.mining.notification.FiletOFortuneNotificationFeature;
import de.vantrex.skysens.client.feature.mining.notification.PowderPumpkinBuffNotificationFeature;
import de.vantrex.skysens.client.feature.qol.SwingAnimationFeature;
import de.vantrex.skysens.client.feature.qol.sensitivity.LocationBasedSensitivityFeature;
import de.vantrex.skysens.client.feature.qol.sensitivity.ZoneBasedSensitivityFeature;
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
        this.featureRegistry.register(new LocationBasedSensitivityFeature());
        this.featureRegistry.register(new ZoneBasedSensitivityFeature());

        this.featureRegistry.register(new FiletOFortuneNotificationFeature());
        this.featureRegistry.register(new PowderPumpkinBuffNotificationFeature());

    }


}
