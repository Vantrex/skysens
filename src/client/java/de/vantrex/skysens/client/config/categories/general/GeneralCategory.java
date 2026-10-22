package de.vantrex.skysens.client.config.categories.general;

import de.vantrex.skysens.client.skyblock.profit.config.ProfitTrackerAccordion;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.notification.NotificationDisplayTypeEnum;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class GeneralCategory {

    @ConfigOption(name = "Notification Display Type", desc = "The type of display to use for notifications")
    @ConfigEditorDropdown
    @Expose
    public NotificationDisplayTypeEnum notificationDisplayType = NotificationDisplayTypeEnum.TITLE;

    @ConfigOption(name = "Notification Sound", desc = "Play a sound when a notification is displayed")
    @ConfigEditorBoolean
    @Expose
    public boolean playSound;

    @ConfigOption(name = "Hold Notifications While AFK", desc = "Keep notifications back while you have not moved for a minute, and show them once you move again")
    @ConfigEditorBoolean
    @Expose
    public boolean holdNotificationsWhileAfk = true;

    @ConfigOption(name = "Profit Tracker", desc = "Session and all-time profit of drops and reward chests, shown by !profit")
    @Accordion
    @Expose
    public ProfitTrackerAccordion profitTracker = new ProfitTrackerAccordion();

}
