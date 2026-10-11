package de.vantrex.skysens.client.config.categories.visuals;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HeldItemAccordion {

    @ConfigOption(name = "Enabled", desc = "Enables first person held item transforms")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Offset X", desc = "Horizontal offset of the held item")
    @ConfigEditorSlider(minValue = -1.5f, maxValue = 1.5f, minStep = 0.01f)
    @Expose
    public float offsetX = 0f;

    @ConfigOption(name = "Offset Y", desc = "Vertical offset of the held item")
    @ConfigEditorSlider(minValue = -1.5f, maxValue = 1.5f, minStep = 0.01f)
    @Expose
    public float offsetY = 0f;

    @ConfigOption(name = "Offset Z", desc = "Depth offset of the held item")
    @ConfigEditorSlider(minValue = -1.5f, maxValue = 1.5f, minStep = 0.01f)
    @Expose
    public float offsetZ = 0f;

    @ConfigOption(name = "Rotation X", desc = "Pitch rotation of the held item")
    @ConfigEditorSlider(minValue = -180f, maxValue = 180f, minStep = 1f)
    @Expose
    public float rotationX = 0f;

    @ConfigOption(name = "Rotation Y", desc = "Yaw rotation of the held item")
    @ConfigEditorSlider(minValue = -180f, maxValue = 180f, minStep = 1f)
    @Expose
    public float rotationY = 0f;

    @ConfigOption(name = "Rotation Z", desc = "Roll rotation of the held item")
    @ConfigEditorSlider(minValue = -180f, maxValue = 180f, minStep = 1f)
    @Expose
    public float rotationZ = 0f;

    @ConfigOption(name = "Scale", desc = "Scale of the held item")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f)
    @Expose
    public float scale = 1f;

}
