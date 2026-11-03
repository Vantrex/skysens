package de.vantrex.skysens.client.features.fishing.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** The Trophy Frog HUD. */
public class TrophyFrogAccordion {

    @ConfigOption(name = "Trophy Frog HUD", desc = "Show how many of each Trophy Frog you caught per tier. There is no API for this: open the Trophy Frogs menu once per profile to load your counts, after that catches from chat are added")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Only On Lotus Atoll", desc = "Only show the HUD on the Lotus Atoll, where Trophy Frogs are caught")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyOnLotusAtoll = true;

    @ConfigOption(name = "Show Total", desc = "Add a column with the total catches of each frog")
    @ConfigEditorBoolean
    @Expose
    public boolean showTotal = true;

    @ConfigOption(name = "Highlight New Catches", desc = "Show a just caught tier in green for a few seconds")
    @ConfigEditorBoolean
    @Expose
    public boolean highlightNew = true;

    @ConfigOption(name = "Hide Completed Frogs", desc = "Hide frogs you already caught in Diamond tier")
    @ConfigEditorBoolean
    @Expose
    public boolean hideCompleted = false;

    @ConfigOption(name = "Strike Uncatchable Frogs", desc = "On the Lotus Atoll, strike through frogs you can't catch right now: wrong zone, no rain, no Red Sweater, not underwater, midair or on a leaf. Wormholes and Frogcoin blessings can't be detected, those frogs are only struck in the Lotus Highlands. Open the chat and hover a frog in the HUD to see its requirements")
    @ConfigEditorBoolean
    @Expose
    public boolean strikeUncatchable = true;

    @ConfigOption(name = "Position X (%)", desc = "Horizontal position of the HUD, in percent of the screen width")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 0.5f)
    @Expose
    public float x = 1f;

    @ConfigOption(name = "Position Y (%)", desc = "Vertical position of the HUD, in percent of the screen height")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 0.5f)
    @Expose
    public float y = 25f;

    @ConfigOption(name = "Scale", desc = "Size of the HUD")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.05f)
    @Expose
    public float scale = 1f;

}
