package de.vantrex.skysens.client.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** How urgent a notification is. The constant names are stored in the config, don't rename them. */
@Getter
@RequiredArgsConstructor
public enum NotificationLevelEnum {

    /** Waits while notifications are held (AFK, combat, dungeons and Kuudra, slayer quests). */
    NORMAL("Normal"),
    /** Ignores those holds and is shown before any queued normal notification. */
    IMPORTANT("Important");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }

}
