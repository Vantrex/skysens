package de.vantrex.skysens.client.config.categories.dungeon;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DungeonCategory {

    @ConfigOption(name = "Chest Profit Tracker", desc = "Track the profit of opened reward chests (dungeons and Kuudra)")
    @Accordion
    @Expose
    public ChestProfitAccordion chestProfit = new ChestProfitAccordion();

    @ConfigOption(name = "Chest Loot Share", desc = "Share the loot of opened reward chests in party or co-op chat")
    @Accordion
    @Expose
    public ChestLootShareAccordion chestLootShare = new ChestLootShareAccordion();

    @ConfigOption(name = "Kismet Reroll Advisor", desc = "Shows whether rerolling a reward chest with a Kismet Feather is worth it")
    @Accordion
    @Expose
    public KismetAdvisorAccordion kismetAdvisor = new KismetAdvisorAccordion();

}
