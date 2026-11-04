package de.vantrex.skysens.client.features.witch.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigEditorMultiSelect;
import de.vantrex.skysens.client.features.witch.WitchIngredient;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

/** The Year of the Witch ingredient HUD. */
public class YearOfTheWitchAccordion {

    @ConfigOption(name = "Ingredient HUD", desc = "Show how many of each Year of the Witch ingredient are in your Witch's Sack. There is no API for this: the amounts are read every time you open your Witch's Sack, and the [Sacks] chat messages add and remove what drops in between (Hypixel's sack notifications must be on). Drops of co-op members, ones that don't go into the sack and ones summed up in a sack message right after you opened it aren't seen until you open it again")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Only During the Event", desc = "Only show the HUD while it is a Year of the Witch (every 12 SkyBlock years)")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyDuringEvent = true;

    @ConfigOption(name = "Shown Ingredients", desc = "The ingredients listed in the HUD. Open the chat and hover one in the HUD to see where it drops")
    @ConfigEditorMultiSelect
    @Expose
    public List<WitchIngredient> shownIngredients = new ArrayList<>(List.of(WitchIngredient.values()));

    @ConfigOption(name = "Show Goal", desc = "Show how many of each ingredient Agnes' shop and Alcina's stew need in total, and color the amount green once you have that many")
    @ConfigEditorBoolean
    @Expose
    public boolean showGoal = true;

    @ConfigOption(name = "Highlight New Drops", desc = "Show an ingredient in green for a few seconds after it went into your sack")
    @ConfigEditorBoolean
    @Expose
    public boolean highlightNew = true;

    @ConfigOption(name = "Position X (%)", desc = "Horizontal position of the HUD, in percent of the screen width")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 0.5f)
    @Expose
    public float x = 1f;

    @ConfigOption(name = "Position Y (%)", desc = "Vertical position of the HUD, in percent of the screen height")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 0.5f)
    @Expose
    public float y = 40f;

    @ConfigOption(name = "Scale", desc = "Size of the HUD")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.05f)
    @Expose
    public float scale = 1f;

}
