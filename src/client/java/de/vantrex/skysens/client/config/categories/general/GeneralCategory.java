package de.vantrex.skysens.client.config.categories.general;

import de.vantrex.skysens.client.enums.NotificationDisplayTypeEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class GeneralCategory {

    @ConfigOption(name = "Notification Display Type", desc = "The type of display to use for notifications")
    @ConfigEditorDropdown
    public NotificationDisplayTypeEnum notificationDisplayType = NotificationDisplayTypeEnum.TITLE;

    @ConfigOption(name = "Notification Sound", desc = "Play a sound when a notification is displayed")
    @ConfigEditorBoolean
    public boolean playSound;

}
