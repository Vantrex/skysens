package de.vantrex.skysens.client.config.categories.general;

import de.vantrex.skysens.client.skyblock.profit.config.ProfitTrackerAccordion;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.notification.NotificationDisplayTypeEnum;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
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

    @ConfigOption(name = "Hold Notifications In Combat", desc = "Keep notifications back while you are fighting, and show them once you stopped")
    @ConfigEditorBoolean
    @Expose
    public boolean holdNotificationsInCombat;

    @ConfigOption(name = "Out of Combat After (seconds)", desc = "Seconds without taking or dealing damage until held notifications are shown")
    @ConfigEditorSlider(minValue = 3f, maxValue = 60f, minStep = 1f)
    @Expose
    public float notificationOutOfCombatSeconds = 10f;

    @ConfigOption(name = "Hold Notifications In Dungeons and Kuudra", desc = "Keep notifications back during a dungeon or Kuudra run, and show them once you left it")
    @ConfigEditorBoolean
    @Expose
    public boolean holdNotificationsInInstances;

    @ConfigOption(name = "Hold Notifications During Slayer Quests", desc = "Keep notifications back while a slayer quest is active, and show them once it is completed, failed or cancelled")
    @ConfigEditorBoolean
    @Expose
    public boolean holdNotificationsDuringSlayerQuest;

    @ConfigOption(name = "Notification Levels", desc = "How urgent each notification is. Important notifications ignore the holds above and are shown first")
    @Accordion
    @Expose
    public NotificationLevelsAccordion notificationLevels = new NotificationLevelsAccordion();

    @ConfigOption(name = "Profit Tracker", desc = "Session and all-time profit of drops and reward chests, shown by !profit")
    @Accordion
    @Expose
    public ProfitTrackerAccordion profitTracker = new ProfitTrackerAccordion();

}
