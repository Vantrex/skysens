package de.vantrex.skysens.client.features.mining.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiningConsumableBuffsNotificationAccordion {

    @ConfigOption(name = "Filet O' Fortune", desc = "Notify when Filet O' Fortune buff is expiring")
    @ConfigEditorBoolean
    @Expose
    public boolean filetOFortune = false;

    @ConfigOption(name = "Filet O' Fortune Level", desc = "Level of the Filet O' Fortune buff notifications (started, about to expire, expired)." + NotificationLevelEnum.LEVELS_DESC)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum filetOFortuneLevel = NotificationLevelEnum.NORMAL;

    @ConfigOption(name = "Powder Pumpkin", desc = "Notify when Powder Pumpkin buff is expiring")
    @ConfigEditorBoolean
    @Expose
    public boolean powderPumpkin = false;

    @ConfigOption(name = "Powder Pumpkin Level", desc = "Level of the Powder Pumpkin buff notifications (started, about to expire, expired)." + NotificationLevelEnum.LEVELS_DESC)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum powderPumpkinLevel = NotificationLevelEnum.NORMAL;

}
