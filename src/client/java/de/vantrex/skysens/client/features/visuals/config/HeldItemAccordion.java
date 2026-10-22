package de.vantrex.skysens.client.features.visuals.config;

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

    public void copyFrom(HeldItemAccordion other) {
        this.enabled = other.enabled;
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
        this.offsetZ = other.offsetZ;
        this.rotationX = other.rotationX;
        this.rotationY = other.rotationY;
        this.rotationZ = other.rotationZ;
        this.scale = other.scale;
    }

    public void clamp() {
        this.offsetX = Math.clamp(this.offsetX, -1.5f, 1.5f);
        this.offsetY = Math.clamp(this.offsetY, -1.5f, 1.5f);
        this.offsetZ = Math.clamp(this.offsetZ, -1.5f, 1.5f);
        this.rotationX = Math.clamp(this.rotationX, -180f, 180f);
        this.rotationY = Math.clamp(this.rotationY, -180f, 180f);
        this.rotationZ = Math.clamp(this.rotationZ, -180f, 180f);
        this.scale = Math.clamp(this.scale, 0.1f, 3f);
    }

}
