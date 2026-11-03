package de.vantrex.skysens.client.features.fishing.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FishingCategory {

    @Accordion
    @ConfigOption(name = "Trophy Frogs", desc = "A HUD with your Trophy Frog catches per tier")
    @Expose
    public TrophyFrogAccordion trophyFrogs = new TrophyFrogAccordion();

}
