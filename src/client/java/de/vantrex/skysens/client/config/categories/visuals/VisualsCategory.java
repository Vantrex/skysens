package de.vantrex.skysens.client.config.categories.visuals;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class VisualsCategory {

    @ConfigOption(name = "Player Scale", desc = "Scale yourself and other players")
    @Accordion
    @Expose
    public PlayerScaleAccordion playerScale = new PlayerScaleAccordion();

    @ConfigOption(name = "NPC Scale", desc = "Scale SkyBlock NPCs and villagers")
    @Accordion
    @Expose
    public NpcScaleAccordion npcScale = new NpcScaleAccordion();

}
