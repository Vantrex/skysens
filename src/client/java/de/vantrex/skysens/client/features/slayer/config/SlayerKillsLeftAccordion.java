package de.vantrex.skysens.client.features.slayer.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SlayerKillsLeftAccordion {

    @ConfigOption(name = "Action Bar", desc = "After slayer quest progress increases, show the combat XP and estimated kills left until the boss spawns")
    @ConfigEditorBoolean
    @Expose
    public boolean actionBar = true;

    @ConfigOption(name = "Action Bar Duration (seconds)", desc = "How long the kills-left estimate stays above the action bar")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    @Expose
    public float actionBarSeconds = 3f;

}