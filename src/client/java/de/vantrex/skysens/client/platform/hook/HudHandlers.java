package de.vantrex.skysens.client.platform.hook;

import de.vantrex.skysens.client.features.actionbar.ActionBarLayout;
import de.vantrex.skysens.client.notification.SecondaryActionBar;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.resources.Identifier;

/** HUD elements SkySens draws on top of the vanilla HUD. */
public class HudHandlers {

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.OVERLAY_MESSAGE, HudHandlers::lifted);
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("skysens", "secondary_action_bar"),
                lifted(SecondaryActionBar.getInstance()::draw));

        // Inventory HUD+ changes its position in its config screens, so look again once one closes
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen.getClass().getName().startsWith("dlovin.inventoryhud.")) {
                ScreenEvents.remove(screen).register(closed -> ActionBarLayout.getInstance().markDirty());
            }
        });
    }

    /** Draws the element moved up by {@link ActionBarLayout#lift}, so it isn't covered by Inventory HUD+. */
    private static HudElement lifted(HudElement element) {
        return (graphics, deltaTracker) -> {
            final int lift = ActionBarLayout.getInstance().lift(graphics.guiWidth(), graphics.guiHeight());
            if (lift == 0) {
                element.extractRenderState(graphics, deltaTracker);
                return;
            }
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, -lift);
            try {
                element.extractRenderState(graphics, deltaTracker);
            } finally {
                graphics.pose().popMatrix();
            }
        };
    }

}
