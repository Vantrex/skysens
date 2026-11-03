package de.vantrex.skysens.client.core.feature;

import de.vantrex.skysens.client.core.hud.MovableHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Draws on top of the vanilla HUD. Called every frame for active features, only while on SkyBlock. */
public interface HudRenderFeature extends Feature {

    void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

    /**
     * The HUD elements of this feature that can be moved in the HUD editor ({@code /skysens hud}), also while the
     * feature is inactive. Return the same instances every time.
     */
    default List<MovableHud> movableHuds() {
        return List.of();
    }

}
