package de.vantrex.skysens.client.config.categories.mining;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.categories.mining.notifcations.MiningNotificationAccordion;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiningCategory {

    @Accordion
    @ConfigOption(name = "Mining Notifications", desc = "Settings related to mining notifications")
    @Expose
    public MiningNotificationAccordion notifications = new MiningNotificationAccordion();

}
