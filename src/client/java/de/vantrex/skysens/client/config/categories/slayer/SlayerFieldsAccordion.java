package de.vantrex.skysens.client.config.categories.slayer;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SlayerFieldsAccordion {

    @ConfigOption(name = "Bosses per Hour", desc = "Killed bosses per hour of questing")
    @ConfigEditorBoolean
    @Expose
    public boolean bossesPerHour = true;

    @ConfigOption(name = "Average Kill Time", desc = "Average time from the boss spawning to the quest completing")
    @ConfigEditorBoolean
    @Expose
    public boolean averageKillTime = true;

    @ConfigOption(name = "Average Quest Time", desc = "Average time from the quest starting to completing, including killing the minibosses")
    @ConfigEditorBoolean
    @Expose
    public boolean averageQuestTime = false;

    @ConfigOption(name = "Failed Quests", desc = "Number of failed quests")
    @ConfigEditorBoolean
    @Expose
    public boolean failed = true;

    @ConfigOption(name = "Profit", desc = "Slayer profit per boss and per hour, from the profit tracker (needs it enabled; with SkyHanni as tracker source it has the slayer spawn costs subtracted)")
    @ConfigEditorBoolean
    @Expose
    public boolean profit = true;

    @ConfigOption(name = "RNG Meter", desc = "Stored XP of the RNG meter, as of the last quest completion")
    @ConfigEditorBoolean
    @Expose
    public boolean rngMeter = false;

}
