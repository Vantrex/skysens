package de.vantrex.skysens.client.features.visuals.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SwingAnimationAccordion {

    @ConfigOption(name = "Enable custom swing animation speed", desc = "Enables custom swing animation speed")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Swing animation speed", desc = "Custom swing animation speed")
    @ConfigEditorSlider(minValue = -10, maxValue = 10, minStep = 1)
    @Expose
    public int swingSpeed = 5;

    @ConfigOption(name = "Apply to all entities", desc = "Apply to all entities. Off means only your own player.")
    @ConfigEditorBoolean
    @Expose
    public boolean applyToAllEntities = false;

    @ConfigOption(name = "Ignore mining effects", desc = "Ignore Haste / Mining Fatigue when the custom speed is enabled.")
    @ConfigEditorBoolean
    @Expose
    public boolean ignoreMiningEffects = true;

    @ConfigOption(name = "Disable swing animation", desc = "Completely disables the swing animation")
    @ConfigEditorBoolean
    @Expose
    public boolean disableSwingAnimation = false;

    public void copyFrom(SwingAnimationAccordion other) {
        this.enabled = other.enabled;
        this.swingSpeed = other.swingSpeed;
        this.applyToAllEntities = other.applyToAllEntities;
        this.ignoreMiningEffects = other.ignoreMiningEffects;
        this.disableSwingAnimation = other.disableSwingAnimation;
    }

    public void clamp() {
        this.swingSpeed = Math.clamp(this.swingSpeed, -10, 10);
    }

}
