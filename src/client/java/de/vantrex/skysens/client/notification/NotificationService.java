package de.vantrex.skysens.client.notification;

import de.vantrex.skysens.client.skyblock.location.LocationService;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.general.GeneralCategory;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.player.AfkTracker;
import de.vantrex.skysens.client.skyblock.player.CombatTracker;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.ScoreboardUtil;
import lombok.Getter;
import lombok.Synchronized;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    @Getter
    private final static NotificationService instance = new NotificationService();

    private final Queue<Notification> notificationQueue = new PriorityQueue<>();
    private final LocationService locationService = LocationService.getInstance();
    private final SkysensClient skysensClient = SkysensClient.getInstance();

    private @Nullable CurrentNotification currentNotification;
    private Long notificationGracePeriod = null;

    private NotificationService() {
        this.init();
    }


    @Synchronized
    public void sendNotification(final @NotNull Notification text) {
        this.notificationQueue.add(text);
    }

    @Synchronized
    public boolean hasNextNotification() {
        return !this.notificationQueue.isEmpty();
    }


    @Synchronized
    private @Nullable Notification getNextNotification() {
        return this.notificationQueue.poll();
    }

    private void handleNotifications() {
        if (!skysensClient.isOnSkyBlock()) {
            return;
        }
        if (this.notificationGracePeriod != null && System.currentTimeMillis() < this.notificationGracePeriod ) {
            return;
        }
        if (this.handleCurrentNotification()) {
            return;
        }
        if (!this.hasNextNotification() || this.isOnHold()) {
            return;
        }
        final Notification nextNotification = this.getNextNotification();
        if (nextNotification == null) {
            return;
        }
        final var displayUntil = System.currentTimeMillis() + this.getDisplayTime(nextNotification);
        this.currentNotification = new CurrentNotification(nextNotification, displayUntil);
        this.displayNotification(nextNotification, true);
    }

    /** Whether queued notifications wait, e.g. while the player is AFK or fighting. They are shown once it is over. */
    private boolean isOnHold() {
        final GeneralCategory config = SkysensConfig.getInstance().generalCategory;
        if (config.holdNotificationsWhileAfk && AfkTracker.getInstance().isAfk()) {
            return true;
        }
        if (config.holdNotificationsInCombat
                && !CombatTracker.getInstance().isOutOfCombatFor(config.notificationOutOfCombatSeconds)) {
            return true;
        }
        if (config.holdNotificationsInDungeons && this.locationService.getCurrentLocation() == SkyblockLocationEnum.CATACOMBS) {
            return true;
        }
        return config.holdNotificationsDuringSlayerQuest
                && ScoreboardUtil.getSidebarLines().stream().anyMatch(line -> line.startsWith("Slayer Quest"));
    }

    private long getDisplayTime(final Notification notification) {
        final Duration duration = notification.getDisplayTime();
        return duration == null ? 2000L : duration.toMillis();
    }

    private boolean handleCurrentNotification() {
        if (this.currentNotification == null) {
            return false;
        }
        if (this.currentNotification.displayUntil() <= System.currentTimeMillis()) {
            this.notificationGracePeriod = System.currentTimeMillis() + 2000L;
            this.currentNotification = null;
            return false;
        }

        this.displayNotification(this.currentNotification.notification(), false);
        return true;
    }

    private void displayNotification(Notification notification, boolean first) {
        final NotificationDisplayTypeEnum displayType = this.getDisplayType(notification);
        final NotificationSound sound = notification.getSound();
        if (first && sound != null) {
            ClientUtil.playSound(sound.sound(), sound.volume(), sound.pitch());
        }
        if (!first && displayType == NotificationDisplayTypeEnum.CHAT) {
            return;
        }
        switch (displayType) {
            case CHAT -> ClientUtil.sendMessage(notification.getText());
            case TITLE -> ClientUtil.sendTitle(notification.getText(), notification.getSubtitle(), 0, 25, 10);
            case ACTION_BAR -> ClientUtil.sendActionBar(notification.getText());
        }
    }

    private @NotNull NotificationDisplayTypeEnum getDisplayType(final @NotNull Notification notification) {
        if (notification.getDisplayType() != null) {
            return notification.getDisplayType();
        }
        return SkysensConfig.getInstance().generalCategory.notificationDisplayType;
    }

    private void init() {
        // The scheduler only provides the timing; the work runs on the client thread, because it
        // touches the player, the HUD and the notification state, none of which are thread-safe.
        SkysensClient.SCHEDULER.scheduleAtFixedRate(
                () -> Minecraft.getInstance().execute(this::handleNotifications),
                500, 500, TimeUnit.MILLISECONDS);
    }

    public record CurrentNotification(Notification notification, long displayUntil) {
    }

}
