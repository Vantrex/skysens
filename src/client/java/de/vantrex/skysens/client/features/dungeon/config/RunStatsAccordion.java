package de.vantrex.skysens.client.features.dungeon.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RunStatsAccordion {

    @ConfigOption(name = "Enabled", desc = "Track the completed dungeon runs per floor (time, score, deaths and, with Chest Profit enabled, reward chest profit) for this session, today and all time. See the !runs chat command")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Run summary in chat", desc = "After every run, show its time and score and the session average in chat (only for you)")
    @ConfigEditorBoolean
    @Expose
    public boolean chatSummary = false;

    @ConfigOption(name = "!runs: Times", desc = "Include the average and best run time in the !runs reply")
    @ConfigEditorBoolean
    @Expose
    public boolean replyTimes = true;

    @ConfigOption(name = "!runs: Score", desc = "Include the S+ rate in the !runs reply")
    @ConfigEditorBoolean
    @Expose
    public boolean replyScore = true;

    @ConfigOption(name = "!runs: Deaths", desc = "Include your deaths per run in the !runs reply")
    @ConfigEditorBoolean
    @Expose
    public boolean replyDeaths = true;

    @ConfigOption(name = "!runs: Chest profit", desc = "Include the floor's reward chest profit, per run and per hour of run time, in the !runs reply. Needs Chest Profit enabled; chests opened at Croesus count for the run's floor")
    @ConfigEditorBoolean
    @Expose
    public boolean replyProfit = true;

}
