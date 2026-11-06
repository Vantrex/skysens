package de.vantrex.skysens.client.features.macro.action;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.notification.Notification;
import de.vantrex.skysens.client.notification.NotificationService;

/** Shows a notification from a macro. */
public final class MacroToasts {

    private MacroToasts() {
    }

    public static void show(Notification notification) {
        if (SkysensClient.getInstance().isOnSkyBlock()) {
            NotificationService.getInstance().sendNotification(notification);
        } else {
            // Only reachable with the dev bypass: the queue never runs off SkyBlock, so show it directly.
            NotificationService.getInstance().displayNow(notification);
        }
    }

}
