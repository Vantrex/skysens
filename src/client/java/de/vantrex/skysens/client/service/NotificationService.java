package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.enums.NotificationDisplayTypeEnum;
import de.vantrex.skysens.client.model.Notification;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import lombok.Synchronized;
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
        if (this.notificationGracePeriod != null || System.currentTimeMillis() < this.notificationGracePeriod ) {
            return;
        }
        if (this.handleCurrentNotification()) {
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
        if (!first && displayType == NotificationDisplayTypeEnum.CHAT) {
            return;
        }
        switch (displayType) {
            case CHAT -> ClientUtil.sendMessage(notification.getText());
            case TITLE -> ClientUtil.sendTitle(notification.getText(), notification.getSubtitle());
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
        SkysensClient.SCHEDULER.scheduleAtFixedRate(this::handleNotifications, 5, 500, TimeUnit.MILLISECONDS);
    }

    public record CurrentNotification(Notification notification, long displayUntil) {
    }

}
