package de.vantrex.skysens.client.handler;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.service.FeatureService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class UseHandlers {

    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();

    public static void register() {
        UseItemCallback.EVENT
                .register((playerEntity, world, hand) -> {
                    handleItemRightClick(playerEntity, hand);
                    return ActionResult.PASS;
                });
    }

    private static void handleItemRightClick(final PlayerEntity playerEntity, final Hand hand) {
        if (!SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        ClientUtil.sendDebug("Right click detected");
        final ItemStack itemStack = playerEntity.getStackInHand(hand);
        ClientUtil.sendDebug("Item in hand: " + itemStack.getItem().getName().getString());
        ClientUtil.sendDebug("feature list size: " + FEATURE_REGISTRY.getItemRightClickFeature().size());
        for (final var feature : FEATURE_REGISTRY.getItemRightClickFeature()) {
            ClientUtil.sendDebug("Checking feature: " + feature.getClass().getSimpleName());
            if (feature.isActive()) {
                feature.onItemRightClick(itemStack);
            }
        }
    }

}
