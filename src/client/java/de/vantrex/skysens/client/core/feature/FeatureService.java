package de.vantrex.skysens.client.core.feature;

import de.vantrex.skysens.client.features.activity.ActivityTrackingFeature;
import de.vantrex.skysens.client.features.bazaar.BazaarDisableClickFeature;
import de.vantrex.skysens.client.skyblock.player.AfkTracker;
import de.vantrex.skysens.client.skyblock.player.CombatTracker;
import de.vantrex.skysens.client.skyblock.slayer.SlayerTracker;
import de.vantrex.skysens.client.features.bazaar.BazaarHighlightFeature;
import de.vantrex.skysens.client.chatcommand.ChatCommandFeature;
import de.vantrex.skysens.client.features.lootshare.LootShareFeature;
import de.vantrex.skysens.client.features.lootshare.SackProfitFeature;
import de.vantrex.skysens.client.features.warp.WarpToFeature;
import de.vantrex.skysens.client.features.warp.WarpToMenuFeature;
import de.vantrex.skysens.client.features.dungeon.DungeonRunDetector;
import de.vantrex.skysens.client.features.dungeon.DungeonRunStatsFeature;
import de.vantrex.skysens.client.features.dungeon.DungeonNpcProfitFeature;
import de.vantrex.skysens.client.features.dungeon.RewardChestFeature;
import de.vantrex.skysens.client.features.mining.FiletOFortuneNotificationFeature;
import de.vantrex.skysens.client.features.mining.PickaxeAbilityResetNotificationFeature;
import de.vantrex.skysens.client.features.mining.PowderPumpkinBuffNotificationFeature;
import de.vantrex.skysens.client.features.junkhighlight.JunkHighlightFeature;
import de.vantrex.skysens.client.features.visuals.SwingAnimationFeature;
import de.vantrex.skysens.client.features.visuals.CameraTweaksFeature;
import de.vantrex.skysens.client.features.visuals.EntityScaleFeature;
import de.vantrex.skysens.client.features.visuals.HideOtherPlayersFeature;
import de.vantrex.skysens.client.features.visuals.HeldItemTransformFeature;
import de.vantrex.skysens.client.features.sensitivity.LocationBasedSensitivityFeature;
import de.vantrex.skysens.client.features.sensitivity.ZoneBasedSensitivityFeature;
import de.vantrex.skysens.client.features.bazaar.BazaarOrderTrackingFeature;
import de.vantrex.skysens.client.features.slayer.SlayerTrackingFeature;
import de.vantrex.skysens.client.features.slayer.rng.SlayerRngMeterFeature;
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
        this.featureRegistry.register(new ActivityTrackingFeature());
        this.featureRegistry.register(new BazaarOrderTrackingFeature());
        this.featureRegistry.register(DungeonRunDetector.getInstance());
        this.featureRegistry.register(new DungeonRunStatsFeature());
        this.featureRegistry.register(new SlayerTrackingFeature());
        this.featureRegistry.register(new SlayerRngMeterFeature());
        this.featureRegistry.register(new de.vantrex.skysens.client.features.dungeon.KismetAdvisorFeature());
        this.featureRegistry.register(new DungeonNpcProfitFeature());
        this.featureRegistry.register(new de.vantrex.skysens.client.features.chatfilter.ChatSpamFilterFeature());
        this.featureRegistry.register(new JunkHighlightFeature());
        this.featureRegistry.register(CombatTracker.getInstance());
        this.featureRegistry.register(AfkTracker.getInstance());
        this.featureRegistry.register(SlayerTracker.getInstance());

    }


}
