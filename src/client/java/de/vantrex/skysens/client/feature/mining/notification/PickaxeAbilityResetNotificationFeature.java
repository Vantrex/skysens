package de.vantrex.skysens.client.feature.mining.notification;

import com.mojang.authlib.GameProfile;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.model.Notification;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.service.NotificationService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.LocationUtil;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.regex.Pattern;

@SkySensFeature
public class PickaxeAbilityResetNotificationFeature implements GameMessageListeningFeature {

    private static final Pattern MINING_ABILITY_PATTERN = Pattern.compile("^\\S+\\s+is now available!$");

    private final NotificationService notificationService = NotificationService.getInstance();
    private final LocationService locationService = LocationService.getInstance();

    @Override
    public boolean isActive() {
        return LocationUtil.isOnMiningRelatedIsland(locationService.getCurrentLocation())
                && SkysensConfig.getInstance().miningCategory.notifications.pickaxeAbility;
    }


    @Override
    public void onGameMessage(Text message, boolean overlay) {
        final String messageAsString = message.getString();
        if (MINING_ABILITY_PATTERN.matcher(messageAsString).matches()) {
            notificationService.sendNotification(Notification.builder()
                    .text(this.buildTitle())
                    .build());
        }
    }

    private Text buildTitle() {
        return Text
                .literal("Mining Ability available!")
                .fillStyle(Style.EMPTY.withColor(Formatting.GREEN));
    }

}
