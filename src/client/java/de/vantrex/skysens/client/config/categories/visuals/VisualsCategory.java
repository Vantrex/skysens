package de.vantrex.skysens.client.config.categories.visuals;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class VisualsCategory {

    @ConfigOption(name = "Player Scale", desc = "Scale yourself and other players")
    @Accordion
    @Expose
    public PlayerScaleAccordion playerScale = new PlayerScaleAccordion();

    @ConfigOption(name = "NPC Scale", desc = "Scale SkyBlock NPCs and villagers")
    @Accordion
    @Expose
    public NpcScaleAccordion npcScale = new NpcScaleAccordion();

    @ConfigOption(name = "Dropped Item Scale", desc = "Scale items lying on the ground")
    @Accordion
    @Expose
    public DroppedItemScaleAccordion itemScale = new DroppedItemScaleAccordion();

    @ConfigOption(name = "Held Item", desc = "Offset, rotate and scale the first person held item")
    @Accordion
    @Expose
    public HeldItemAccordion heldItem = new HeldItemAccordion();

    @ConfigOption(name = "Misc", desc = "Player visibility, camera and crosshair options")
    @Accordion
    @Expose
    public MiscVisualsAccordion misc = new MiscVisualsAccordion();

    public void copyFrom(VisualsCategory other) {
        this.playerScale.copyFrom(other.playerScale);
        this.npcScale.copyFrom(other.npcScale);
        this.itemScale.copyFrom(other.itemScale);
        this.heldItem.copyFrom(other.heldItem);
        this.misc.copyFrom(other.misc);
    }

    public void clamp() {
        this.playerScale.clamp();
        this.npcScale.clamp();
        this.itemScale.clamp();
        this.heldItem.clamp();
        this.misc.clamp();
    }

}
