package de.vantrex.skysens.client.platform.hook;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.feature.FeatureRegistry;
import de.vantrex.skysens.client.core.feature.FeatureService;
import de.vantrex.skysens.client.core.feature.HudRenderFeature;
import de.vantrex.skysens.client.core.hud.HudRegistry;
import de.vantrex.skysens.client.features.actionbar.ActionBarLayout;
import de.vantrex.skysens.client.features.actionbar.SecondaryActionBarHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** HUD elements SkySens draws on top of the vanilla HUD. */
public class HudHandlers {

    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.OVERLAY_MESSAGE, HudHandlers::lifted);
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("skysens", "secondary_action_bar"),
                SecondaryActionBarHud.getInstance()::draw);
        HudRegistry.getInstance().register(SecondaryActionBarHud.getInstance());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("skysens", "features"), HudHandlers::renderFeatures);

        // Inventory HUD+ changes its position in its config screens, so look again once one closes
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen.getClass().getName().startsWith("dlovin.inventoryhud.")) {
                ScreenEvents.remove(screen).register(closed -> ActionBarLayout.getInstance().markDirty());
            }
        });
    }

    private static void renderFeatures(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        // hidden with F1; the HUD editor draws its own previews
        if (!SkysensClient.getInstance().isOnSkyBlock() || Minecraft.getInstance().gui.hud.isHidden()
                || HudRegistry.getInstance().isEditing()) {
            return;
        }
        for (final HudRenderFeature feature : FEATURE_REGISTRY.getHudRenderFeature()) {
            if (feature.isActive()) {
                feature.renderHud(graphics, deltaTracker);
            }
        }
    }

    /**
     * Draws the vanilla action bar moved up by {@link ActionBarLayout#lift}, so it isn't covered by Inventory HUD+.
     * SkySens' own action bar applies the lift itself, see {@link SecondaryActionBarHud}.
     */
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
