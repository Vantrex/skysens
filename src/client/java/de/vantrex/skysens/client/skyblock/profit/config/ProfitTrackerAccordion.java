package de.vantrex.skysens.client.skyblock.profit.config;

import de.vantrex.skysens.client.config.annotation.ConfigMasterToggle;
import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.skyblock.profit.ProfitSourceEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ProfitTrackerAccordion {

    @ConfigOption(name = "Enabled", desc = "Track the value of your rare drops and reward chests, overall and per activity (shown by !profit). See \"Tracker\" for which numbers are used.")
    @ConfigEditorBoolean
    @ConfigMasterToggle
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Tracker", desc = "SkyHanni: use SkyHanni's trackers (every drop incl. sacks, minus slayer spawn costs) when installed. SkySens: only SkySens' own tracker (rare drops and reward chests).")
    @ConfigEditorDropdown
    @Expose
    public ProfitSourceEnum source = ProfitSourceEnum.SKYHANNI;

    @ConfigOption(name = "AFK timeout (minutes)", desc = "Time between two gains of an activity that still counts as playing it, for profit per hour")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    @Expose
    public float afkTimeoutMinutes = 10f;

}
