package de.vantrex.skysens.client.config.categories.misc;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.features.volcano.config.VolcanoEruptionAccordion;
import de.vantrex.skysens.client.features.witch.config.YearOfTheWitchAccordion;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** Features that don't fit another category, e.g. for SkyBlock events. */
public class MiscCategory {

    @Accordion
    @ConfigOption(name = "Year of the Witch", desc = "A HUD with the ingredients in your Witch's Sack")
    @Expose
    public YearOfTheWitchAccordion yearOfTheWitch = new YearOfTheWitchAccordion();

    @Accordion
    @ConfigOption(name = "Volcano Eruption", desc = "A title when the Blazing Volcano on the Crimson Isle erupts")
    @Expose
    public VolcanoEruptionAccordion volcanoEruption = new VolcanoEruptionAccordion();

}
