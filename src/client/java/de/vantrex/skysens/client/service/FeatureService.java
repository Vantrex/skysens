package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.feature.bazaar.BazaarDisableClickFeature;
import de.vantrex.skysens.client.feature.bazaar.BazaarHighlightFeature;
import de.vantrex.skysens.client.feature.chat.ChatCommandFeature;
import de.vantrex.skysens.client.feature.chat.LootShareFeature;
import de.vantrex.skysens.client.feature.chat.SackProfitFeature;
import de.vantrex.skysens.client.feature.chat.WarpToFeature;
import de.vantrex.skysens.client.feature.chat.WarpToMenuFeature;
import de.vantrex.skysens.client.feature.dungeon.RewardChestFeature;
import de.vantrex.skysens.client.feature.mining.notification.FiletOFortuneNotificationFeature;
import de.vantrex.skysens.client.feature.mining.notification.PickaxeAbilityResetNotificationFeature;
import de.vantrex.skysens.client.feature.mining.notification.PowderPumpkinBuffNotificationFeature;
import de.vantrex.skysens.client.feature.qol.JunkHighlightFeature;
import de.vantrex.skysens.client.feature.qol.SwingAnimationFeature;
import de.vantrex.skysens.client.feature.visuals.CameraTweaksFeature;
import de.vantrex.skysens.client.feature.visuals.EntityScaleFeature;
import de.vantrex.skysens.client.feature.visuals.HideOtherPlayersFeature;
import de.vantrex.skysens.client.feature.visuals.HeldItemTransformFeature;
import de.vantrex.skysens.client.feature.qol.sensitivity.LocationBasedSensitivityFeature;
import de.vantrex.skysens.client.feature.qol.sensitivity.ZoneBasedSensitivityFeature;
import lombok.Getter;

@Getter
public class FeatureService {

    @Getter
    private static final FeatureService instance = new FeatureService();

    private final FeatureRegistry featureRegistry = new FeatureRegistry();
    private final RewardChestFeature rewardChestFeature = new RewardChestFeature();

    private FeatureService() {
        this.register();
    }

    public void register() {
        this.featureRegistry.register(new BazaarHighlightFeature());
        this.featureRegistry.register(new BazaarDisableClickFeature());
        this.featureRegistry.register(new SwingAnimationFeature());
        this.featureRegistry.register(new EntityScaleFeature());
        this.featureRegistry.register(new HeldItemTransformFeature());
        this.featureRegistry.register(new CameraTweaksFeature());
        this.featureRegistry.register(new HideOtherPlayersFeature());
        this.featureRegistry.register(new LocationBasedSensitivityFeature());
        this.featureRegistry.register(new ZoneBasedSensitivityFeature());

        this.featureRegistry.register(new FiletOFortuneNotificationFeature());
        this.featureRegistry.register(new PowderPumpkinBuffNotificationFeature());

        this.featureRegistry.register(new PickaxeAbilityResetNotificationFeature());

        this.featureRegistry.register(new ChatCommandFeature());
        this.featureRegistry.register(new WarpToFeature());
        this.featureRegistry.register(new WarpToMenuFeature());
        this.featureRegistry.register(new LootShareFeature());
        this.featureRegistry.register(new SackProfitFeature());
        this.featureRegistry.register(this.rewardChestFeature);
        this.featureRegistry.register(new de.vantrex.skysens.client.feature.chat.ChatSpamFilterFeature());
        this.featureRegistry.register(new JunkHighlightFeature());

    }


}
