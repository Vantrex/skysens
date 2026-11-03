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

    /** Appended to the description of every level option in the config. */
    public static final String LEVELS_DESC = " Normal waits while notifications are held (AFK, combat, dungeons and Kuudra, slayer quests). Important ignores these holds and is shown before queued normal notifications.";

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }

}
