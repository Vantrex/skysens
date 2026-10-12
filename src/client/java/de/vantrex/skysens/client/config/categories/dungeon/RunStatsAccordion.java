package de.vantrex.skysens.client.config.categories.dungeon;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RunStatsAccordion {

    @ConfigOption(name = "Enabled", desc = "Track the completed dungeon runs of this session per floor (time, score, deaths). See the !runs chat command")
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

}
