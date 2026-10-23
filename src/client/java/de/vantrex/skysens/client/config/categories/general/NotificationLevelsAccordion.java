package de.vantrex.skysens.client.config.categories.general;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/**
 * The level of every notification. Normal ones wait while notifications are held, important ones are shown right away
 * and before queued normal ones.
 */
public class NotificationLevelsAccordion {

    private static final String LEVELS = " Normal waits while notifications are held (AFK, combat, dungeons and Kuudra, slayer quests). Important ignores these holds and is shown before queued normal notifications.";

    @ConfigOption(name = "Bazaar: Outbid", desc = "Level of the bazaar outbid alert. Important also skips the order alerts' own waiting for combat and runs." + LEVELS)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum bazaarOutbid = NotificationLevelEnum.NORMAL;

    @ConfigOption(name = "Bazaar: Undercut", desc = "Level of the bazaar undercut alert. Important also skips the order alerts' own waiting for combat and runs." + LEVELS)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum bazaarUndercut = NotificationLevelEnum.NORMAL;

    @ConfigOption(name = "Bazaar: Order Filled", desc = "Level of the bazaar order filled alert." + LEVELS)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum bazaarFilled = NotificationLevelEnum.NORMAL;

    @ConfigOption(name = "Mining: Pickaxe Ability", desc = "Level of the pickaxe ability ready notification." + LEVELS)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum pickaxeAbility = NotificationLevelEnum.NORMAL;

    @ConfigOption(name = "Mining: Filet O' Fortune", desc = "Level of the Filet O' Fortune buff notifications (started, about to expire, expired)." + LEVELS)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum filetOFortune = NotificationLevelEnum.NORMAL;

    @ConfigOption(name = "Mining: Powder Pumpkin", desc = "Level of the Powder Pumpkin buff notifications (started, about to expire, expired)." + LEVELS)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum powderPumpkin = NotificationLevelEnum.NORMAL;

}
