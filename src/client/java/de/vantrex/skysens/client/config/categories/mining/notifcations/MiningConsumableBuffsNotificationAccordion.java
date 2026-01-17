package de.vantrex.skysens.client.config.categories.mining.notifcations;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiningConsumableBuffsNotificationAccordion {

    @ConfigOption(name = "Filet O' Fortune", desc = "Notify when Filet O' Fortune buff is expiring")
    @ConfigEditorBoolean
    @Expose
    public boolean filetOFortune = false;

    @ConfigOption(name = "Powder Pumpkin", desc = "Notify when Powder Pumpkin buff is expiring")
    @ConfigEditorBoolean
    @Expose
    public boolean powderPumpkin = false;

}
