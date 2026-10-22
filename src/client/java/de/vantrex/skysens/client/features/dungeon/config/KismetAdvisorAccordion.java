package de.vantrex.skysens.client.features.dungeon.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class KismetAdvisorAccordion {

    @ConfigOption(name = "Enabled", desc = "In a reward chest, tell you whether rerolling it with a Kismet Feather is worth it")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Reroll below (millions)", desc = "Advise a reroll when the chest's profit (rewards minus open cost) is below this")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 0.5f)
    @Expose
    public float thresholdMillions = 5f;

    @ConfigOption(name = "Only Bedrock chest", desc = "Only advise for the Bedrock chest. Otherwise the chests below are used.")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyBedrock = false;

    @ConfigOption(name = "Obsidian chest", desc = "Also advise for the Obsidian chest")
    @ConfigEditorBoolean
    @Expose
    public boolean obsidian = true;

    @ConfigOption(name = "Other chests", desc = "Also advise for Wood, Iron, Gold, Diamond and Emerald chests")
    @ConfigEditorBoolean
    @Expose
    public boolean otherChests = false;

    @ConfigOption(name = "Highlight reroll button", desc = "Colour the Reroll Chest button green (worth it) or red (keep)")
    @ConfigEditorBoolean
    @Expose
    public boolean highlightButton = true;

    @ConfigOption(name = "Show text", desc = "Show a short line above the chest, e.g. \"Reroll: worth it (chest 1.2M < 5M)\"")
    @ConfigEditorBoolean
    @Expose
    public boolean showText = true;

    @ConfigOption(name = "Chat message", desc = "Also send the advice to chat when you open a chest (only for you)")
    @ConfigEditorBoolean
    @Expose
    public boolean chatMessage = false;

}
