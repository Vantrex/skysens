package de.vantrex.skysens.client.config.categories.qol.sub;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SwingAnimationSubCategory {

    @ConfigOption(name = "Enable custom swing animation speed", desc = "Enables custom swing animation speed")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Swing animation speed", desc = "Custom swing animation speed")
    @ConfigEditorSlider(minValue = -10, maxValue = 10, minStep = 1)
    @Expose
    public int swingSpeed = 5;

}
