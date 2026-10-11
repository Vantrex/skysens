package de.vantrex.skysens.client.config.categories.visuals;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DroppedItemScaleAccordion {

    @ConfigOption(name = "Enabled", desc = "Enables dropped item scaling")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Scale", desc = "Scale of dropped items")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f)
    @Expose
    public float scale = 1f;

    public void copyFrom(DroppedItemScaleAccordion other) {
        this.enabled = other.enabled;
        this.scale = other.scale;
    }

    public void clamp() {
        this.scale = Math.clamp(this.scale, 0.1f, 3f);
    }

}
