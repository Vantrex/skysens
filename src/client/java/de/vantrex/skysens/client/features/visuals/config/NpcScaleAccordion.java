package de.vantrex.skysens.client.features.visuals.config;

import de.vantrex.skysens.client.config.annotation.ConfigMasterToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class NpcScaleAccordion {

    @ConfigOption(name = "Enabled", desc = "Enables NPC scaling. SkyBlock only, always 1x in Dungeons. NPC name holograms are separate armor stands and are not moved.")
    @ConfigEditorBoolean
    @ConfigMasterToggle
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "NPC scale", desc = "Scale of player-like NPCs")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f)
    @Expose
    public float npcScale = 1f;

    @ConfigOption(name = "Villager scale", desc = "Scale of villager NPCs")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f)
    @Expose
    public float villagerScale = 1f;

    public void copyFrom(NpcScaleAccordion other) {
        this.enabled = other.enabled;
        this.npcScale = other.npcScale;
        this.villagerScale = other.villagerScale;
    }

    public void clamp() {
        this.npcScale = Math.clamp(this.npcScale, 0.1f, 3f);
        this.villagerScale = Math.clamp(this.villagerScale, 0.1f, 3f);
    }

}
