package de.vantrex.skysens.client.config.migration;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.bazaar.config.OrderAlerts;
import de.vantrex.skysens.client.features.mining.config.MiningNotificationAccordion;
import de.vantrex.skysens.client.features.slayer.config.SlayerRngMeterAccordion;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The old General > Notification Levels group (generalCategory.notificationLevels) moved next to each feature's options. */
public class LegacyNotificationLevelsConfigMigration implements ConfigMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");

    @Override
    public boolean shouldMigrate(JsonObject root) {
        return root.get("generalCategory") instanceof JsonObject general
                && general.get("notificationLevels") instanceof JsonObject;
    }

    @Override
    public void migrate(JsonObject root, SkysensConfig config) {
        final JsonObject levels = root.getAsJsonObject("generalCategory").getAsJsonObject("notificationLevels");
        final OrderAlerts orderAlerts = config.bazaarCategory.orderAlerts;
        orderAlerts.outbidLevel = legacyLevel(levels, "bazaarOutbid", orderAlerts.outbidLevel);
        orderAlerts.undercutLevel = legacyLevel(levels, "bazaarUndercut", orderAlerts.undercutLevel);
        orderAlerts.filledLevel = legacyLevel(levels, "bazaarFilled", orderAlerts.filledLevel);
        final MiningNotificationAccordion mining = config.miningCategory.notifications;
        mining.pickaxeAbilityLevel = legacyLevel(levels, "pickaxeAbility", mining.pickaxeAbilityLevel);
        mining.consumableBuffs.filetOFortuneLevel = legacyLevel(levels, "filetOFortune", mining.consumableBuffs.filetOFortuneLevel);
        mining.consumableBuffs.powderPumpkinLevel = legacyLevel(levels, "powderPumpkin", mining.consumableBuffs.powderPumpkinLevel);
        final SlayerRngMeterAccordion rngMeter = config.slayerCategory.rngMeter;
        rngMeter.notifyWhenFullLevel = legacyLevel(levels, "slayerRngMeterFull", rngMeter.notifyWhenFullLevel);
    }

    private static NotificationLevelEnum legacyLevel(JsonObject levels, String key, NotificationLevelEnum fallback) {
        try {
            return levels.has(key) ? NotificationLevelEnum.valueOf(levels.get(key).getAsString()) : fallback;
        } catch (Exception e) {
            LOGGER.warn("Could not migrate notification level {}", key, e);
            return fallback;
        }
    }
}
