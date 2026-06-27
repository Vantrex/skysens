package de.vantrex.skysens.client.feature.mining.notification;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.model.Notification;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.service.NotificationService;
import de.vantrex.skysens.client.util.LocationUtil;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.regex.Pattern;

@SkySensFeature
public class PickaxeAbilityResetNotificationFeature implements GameMessageListeningFeature {

    private static final Pattern MINING_ABILITY_PATTERN = Pattern.compile("^(.+) is now available!$");

    private final NotificationService notificationService = NotificationService.getInstance();
    private final LocationService locationService = LocationService.getInstance();

    @Override
    public boolean isActive() {
        return LocationUtil.isOnMiningRelatedIsland(locationService.getCurrentLocation())
                && SkysensConfig.getInstance().miningCategory.notifications.pickaxeAbility;
    }


    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (!isActive()) {
            return;
        }
        final String messageAsString = message.getString();
        if (MINING_ABILITY_PATTERN.matcher(messageAsString).matches()) {
            notificationService.sendNotification(Notification.builder()
                    .text(this.buildTitle())
                    .build());
        }
    }

    private Component buildTitle() {
        return Component
                .literal("Mining Ability available!")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
    }

}
