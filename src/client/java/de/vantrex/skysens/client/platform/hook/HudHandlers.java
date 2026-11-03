package de.vantrex.skysens.client.platform.hook;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.feature.FeatureRegistry;
import de.vantrex.skysens.client.core.feature.FeatureService;
import de.vantrex.skysens.client.core.feature.HudRenderFeature;
import de.vantrex.skysens.client.notification.SecondaryActionBar;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** HUD elements SkySens draws on top of the vanilla HUD. */
public class HudHandlers {

    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("skysens", "secondary_action_bar"),
                SecondaryActionBar.getInstance()::draw);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("skysens", "features"), HudHandlers::renderFeatures);
    }

    private static void renderFeatures(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        // hidden with F1
        if (!SkysensClient.getInstance().isOnSkyBlock() || Minecraft.getInstance().gui.hud.isHidden()) {
            return;
        }
        for (final HudRenderFeature feature : FEATURE_REGISTRY.getHudRenderFeature()) {
            if (feature.isActive()) {
                feature.renderHud(graphics, deltaTracker);
            }
        }
    }

}
