package de.vantrex.skysens.client.feature.visuals;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.visuals.NpcScaleAccordion;
import de.vantrex.skysens.client.config.categories.visuals.PlayerScaleAccordion;
import de.vantrex.skysens.client.feature.EntityRenderFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.util.EntityClassifier;
import net.minecraft.world.entity.Entity;

@SkySensFeature
public class EntityScaleFeature implements EntityRenderFeature {

    private final PlayerScaleAccordion playerScale = SkysensConfig.getInstance().visualsCategory.playerScale;
    private final NpcScaleAccordion npcScale = SkysensConfig.getInstance().visualsCategory.npcScale;

    @Override
    public float getModelScale(Entity entity, EntityClassifier.Kind kind) {
        return switch (kind) {
            case LOCAL_PLAYER -> this.playerScale.enabled ? this.playerScale.selfScale : 1f;
            case OTHER_PLAYER -> this.playerScale.enabled ? this.playerScale.otherPlayersScale : 1f;
            case NPC -> this.npcScale.enabled && EntityClassifier.npcScalingAllowed() ? this.npcScale.npcScale : 1f;
            case VILLAGER ->
                    this.npcScale.enabled && EntityClassifier.npcScalingAllowed() ? this.npcScale.villagerScale : 1f;
            default -> 1f;
        };
    }

    @Override
    public boolean scaleNameTag(Entity entity, EntityClassifier.Kind kind) {
        return this.playerScale.enabled && this.playerScale.scaleNameTags;
    }

    @Override
    public boolean isActive() {
        return this.playerScale.enabled || this.npcScale.enabled;
    }
}
