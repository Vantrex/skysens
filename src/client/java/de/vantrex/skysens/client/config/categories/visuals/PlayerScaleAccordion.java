package de.vantrex.skysens.client.config.categories.visuals;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PlayerScaleAccordion {

    @ConfigOption(name = "Enabled", desc = "Enables player scaling")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Own scale", desc = "Scale of your own player model")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f)
    @Expose
    public float selfScale = 1f;

    @ConfigOption(name = "Other players scale", desc = "Scale of other players")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f)
    @Expose
    public float otherPlayersScale = 1f;

    @ConfigOption(name = "Scale name tags", desc = "Also scales the name tags of scaled players")
    @ConfigEditorBoolean
    @Expose
    public boolean scaleNameTags = false;

}
