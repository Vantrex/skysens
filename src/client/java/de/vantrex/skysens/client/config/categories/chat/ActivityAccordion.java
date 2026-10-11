package de.vantrex.skysens.client.config.categories.chat;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** Settings of the activity timeline behind {@code !activity}. */
public class ActivityAccordion {

    @ConfigOption(name = "Default count", desc = "How many activities !activity shows when no number is given (the reply is trimmed to fit the chat limit)")
    @ConfigEditorSlider(minValue = 1f, maxValue = 8f, minStep = 1f)
    @Expose
    public float defaultCount = 3f;

    @ConfigOption(name = "History (hours)", desc = "How long activities are remembered, also across restarts")
    @ConfigEditorSlider(minValue = 1f, maxValue = 48f, minStep = 1f)
    @Expose
    public float historyHours = 12f;

    @ConfigOption(name = "Minimum visit (minutes)", desc = "Island visits shorter than this are not listed, so hub trips and teleports don't clutter the list")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 0.5f)
    @Expose
    public float minSegmentMinutes = 2f;

    @ConfigOption(name = "Merge gap (minutes)", desc = "Runs, kills or burrows of the same kind within this time of the previous one are merged into one entry (\"M7 x5\")")
    @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
    @Expose
    public float mergeGapMinutes = 15f;

    @ConfigOption(name = "Show current location", desc = "Add \"now: <island>\" to the reply")
    @ConfigEditorBoolean
    @Expose
    public boolean includeCurrent = true;

    @ConfigOption(name = "Track dungeon runs", desc = "Completed dungeon runs, e.g. M7 x5")
    @ConfigEditorBoolean
    @Expose
    public boolean trackDungeons = true;

    @ConfigOption(name = "Track slayer bosses", desc = "Killed slayer bosses, e.g. Inferno Demonlord IV x14")
    @ConfigEditorBoolean
    @Expose
    public boolean trackSlayers = true;

    @ConfigOption(name = "Track Diana burrows", desc = "Griffin burrows dug up during the Diana event")
    @ConfigEditorBoolean
    @Expose
    public boolean trackDiana = true;

    @ConfigOption(name = "Track Kuudra runs", desc = "Defeated Kuudra runs")
    @ConfigEditorBoolean
    @Expose
    public boolean trackKuudra = true;

    @ConfigOption(name = "Track island visits", desc = "Plain stays on an island without any of the activities above")
    @ConfigEditorBoolean
    @Expose
    public boolean trackLocations = true;

}
