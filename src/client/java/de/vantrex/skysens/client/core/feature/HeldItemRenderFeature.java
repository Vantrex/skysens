package de.vantrex.skysens.client.core.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;

public interface HeldItemRenderFeature extends Feature {

    void transformHeldItem(PoseStack poseStack, HumanoidArm arm);

}
