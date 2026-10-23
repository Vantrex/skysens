package de.vantrex.skysens.client.platform.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.vantrex.skysens.client.core.feature.HeldItemRenderFeature;
import de.vantrex.skysens.client.core.feature.FeatureService;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void skysens$heldItem(AbstractClientPlayer player, float partialTick, float xRot, InteractionHand hand, float attack, ItemStack stack,
                                  float equip, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        final HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        for (HeldItemRenderFeature feature : FeatureService.getInstance().getFeatureRegistry().getHeldItemRenderFeature()) {
            if (feature.isActive()) {
                feature.transformHeldItem(poseStack, arm);
            }
        }
    }
}
