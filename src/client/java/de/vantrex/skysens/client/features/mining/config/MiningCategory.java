package de.vantrex.skysens.client.features.mining.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiningCategory {

    @Accordion
    @ConfigOption(name = "Mining Notifications", desc = "Settings related to mining notifications")
    @Expose
    public MiningNotificationAccordion notifications = new MiningNotificationAccordion();

}
