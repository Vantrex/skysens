package de.vantrex.skysens.client.config.categories.mining.notifcations;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiningNotificationAccordion {

    @ConfigOption(name = "Consumable Buffs", desc = "Settings related to consumable buff notifications")
    @Accordion
    @Expose
    public MiningConsumableBuffsNotificationAccordion consumableBuffs = new MiningConsumableBuffsNotificationAccordion();

}
