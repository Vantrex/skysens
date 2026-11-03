package de.vantrex.skysens.client.core.feature;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws on top of the vanilla HUD. Called every frame for active features, only while on SkyBlock. */
public interface HudRenderFeature extends Feature {

    void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

}
