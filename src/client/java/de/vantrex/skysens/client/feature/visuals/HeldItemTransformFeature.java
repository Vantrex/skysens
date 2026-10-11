package de.vantrex.skysens.client.feature.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.visuals.HeldItemAccordion;
import de.vantrex.skysens.client.feature.HeldItemRenderFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import net.minecraft.world.entity.HumanoidArm;

@SkySensFeature
public class HeldItemTransformFeature implements HeldItemRenderFeature {

    private final HeldItemAccordion heldItem = SkysensConfig.getInstance().visualsCategory.heldItem;

    @Override
    public void transformHeldItem(PoseStack poseStack, HumanoidArm arm) {
        // Mirror horizontal offset and yaw/roll for the left arm so settings look the same on both hands.
        final float mirror = arm == HumanoidArm.RIGHT ? 1f : -1f;
        poseStack.translate(mirror * this.heldItem.offsetX, this.heldItem.offsetY, this.heldItem.offsetZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(this.heldItem.rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(mirror * this.heldItem.rotationY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(mirror * this.heldItem.rotationZ));
        poseStack.scale(this.heldItem.scale, this.heldItem.scale, this.heldItem.scale);
    }

    @Override
    public boolean isActive() {
        return this.heldItem.enabled;
    }
}
