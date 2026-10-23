package de.vantrex.skysens.client.features.slayer.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** Settings of the RNG meter tracking. The tracked drop is chosen per slayer in its own section. */
public class SlayerRngMeterAccordion {

    @ConfigOption(name = "Action Bar After Kill", desc = "After a killed boss, show the RNG meter progress of the slayer's tracked drop in the action bar: stored XP, bosses left and whether it is selected in /rng. Open the slayer's RNG meter menu once (/rng) so the selected drop and the exact XP needed are known")
    @ConfigEditorBoolean
    @Expose
    public boolean actionBar = true;

    @ConfigOption(name = "Action Bar Duration (seconds)", desc = "How long the RNG meter progress stays in the action bar")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    @Expose
    public float actionBarSeconds = 4f;

    @ConfigOption(name = "Notify When Full", desc = "Notify once the RNG meter has enough XP for the tracked drop and the last boss' tier can drop it. Its level is set in General > Notification Levels (Important by default, so it shows even in combat or during a slayer quest)")
    @ConfigEditorBoolean
    @Expose
    public boolean notifyWhenFull = true;

}
