package de.vantrex.skysens.client.features.dungeon.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ChestProfitAccordion {

    @ConfigOption(name = "Enabled", desc = "Track revenue, cost and profit of every reward chest you open. See /skysens dungeon profit")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Chat summary", desc = "After opening a chest, show its profit and the session total in chat (only for you)")
    @ConfigEditorBoolean
    @Expose
    public boolean chatSummary = true;

    @ConfigOption(name = "Count NPC sales", desc = "Add the coins from selling items to an NPC (\"You sold ...\") to your profit")
    @ConfigEditorBoolean
    @Expose
    public boolean npcSales = true;

    @ConfigOption(name = "NPC sales only in dungeons", desc = "Only count NPC sales in the Dungeon Hub and dungeons. When off, sales anywhere count.")
    @ConfigEditorBoolean
    @Expose
    public boolean npcSalesDungeonsOnly = true;

    @ConfigOption(name = "Count salvaged essence", desc = "Add the value of essence from salvaging dungeon gear to your dungeon profit")
    @ConfigEditorBoolean
    @Expose
    public boolean salvage = true;

}
