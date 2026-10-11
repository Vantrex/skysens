package de.vantrex.skysens.client.config.categories.dungeon;

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

}
