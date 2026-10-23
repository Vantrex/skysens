package de.vantrex.skysens.client.features.slayer.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.features.slayer.rng.drop.TarantulaRngDropEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class TarantulaBroodfatherAccordion {

    @ConfigOption(name = "Tracked RNG Drop", desc = "The Tarantula Broodfather drop to track on the RNG meter: after every boss the action bar shows how many bosses are left until the meter is full for it, and whether it is selected in /rng. It doesn't have to be selected in /rng, but only a selected drop is guaranteed once the meter is full. Opening /rng for this slayer syncs the selected drop to this setting")
    @ConfigEditorDropdown
    @Expose
    public TarantulaRngDropEnum rngDrop = TarantulaRngDropEnum.NONE;

}
