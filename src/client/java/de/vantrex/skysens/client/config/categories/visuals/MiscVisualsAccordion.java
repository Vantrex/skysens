package de.vantrex.skysens.client.config.categories.visuals;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiscVisualsAccordion {

    @ConfigOption(name = "Hide other players", desc = "Hides all other players (NPCs stay visible)")
    @ConfigEditorBoolean
    @Expose
    public boolean hideOtherPlayers = false;

    @ConfigOption(name = "Disable selfie camera", desc = "Skips the front facing third person camera when cycling perspectives")
    @ConfigEditorBoolean
    @Expose
    public boolean disableSelfieCam = false;

    @ConfigOption(name = "Third person crosshair", desc = "Shows the crosshair in third person. The crosshair is drawn at the screen center while the hit result is traced from your eyes.")
    @ConfigEditorBoolean
    @Expose
    public boolean thirdPersonCrosshair = false;

    public void copyFrom(MiscVisualsAccordion other) {
        this.hideOtherPlayers = other.hideOtherPlayers;
        this.disableSelfieCam = other.disableSelfieCam;
        this.thirdPersonCrosshair = other.thirdPersonCrosshair;
    }

    public void clamp() {
    }

}
