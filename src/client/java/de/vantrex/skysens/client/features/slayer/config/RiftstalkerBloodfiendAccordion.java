package de.vantrex.skysens.client.features.slayer.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.features.slayer.rng.drop.RiftstalkerRngDropEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RiftstalkerBloodfiendAccordion {

    @ConfigOption(name = "Tracked RNG Drop", desc = "The Riftstalker Bloodfiend drop to track on the RNG meter: after every boss the action bar shows how many bosses are left until the meter is full for it, and whether it is selected in /rng. It doesn't have to be selected in /rng, but only a selected drop is guaranteed once the meter is full")
    @ConfigEditorDropdown
    @Expose
    public RiftstalkerRngDropEnum rngDrop = RiftstalkerRngDropEnum.NONE;

}
