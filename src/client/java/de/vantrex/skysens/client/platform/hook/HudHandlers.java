package de.vantrex.skysens.client.platform.hook;

import de.vantrex.skysens.client.notification.SecondaryActionBar;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/** HUD elements SkySens draws on top of the vanilla HUD. */
public class HudHandlers {

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("skysens", "secondary_action_bar"),
                SecondaryActionBar.getInstance()::draw);
    }

}
