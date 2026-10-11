package de.vantrex.skysens.client.config.categories.general;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ProfitTrackerAccordion {

    @ConfigOption(name = "Enabled", desc = "Track the value of your rare drops and reward chests, overall and per activity (shown by !profit). Uses SkyHanni's trackers for activities when installed.")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "AFK timeout (minutes)", desc = "Time between two gains of an activity that still counts as playing it, for profit per hour")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    @Expose
    public float afkTimeoutMinutes = 10f;

}
