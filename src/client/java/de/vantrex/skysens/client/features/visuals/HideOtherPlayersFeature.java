package de.vantrex.skysens.client.features.visuals;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.visuals.config.MiscVisualsAccordion;
import de.vantrex.skysens.client.core.feature.EntityRenderFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.skyblock.entity.EntityClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

@SkySensFeature
public class HideOtherPlayersFeature implements EntityRenderFeature {

    private final MiscVisualsAccordion misc = SkysensConfig.getInstance().visualsCategory.misc;

    @Override
    public boolean shouldHide(Entity entity, EntityClassifier.Kind kind) {
        return kind == EntityClassifier.Kind.OTHER_PLAYER && entity != Minecraft.getInstance().getCameraEntity();
    }

    @Override
    public boolean isActive() {
        return this.misc.hideOtherPlayers;
    }
}
