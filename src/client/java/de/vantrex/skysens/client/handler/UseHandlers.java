package de.vantrex.skysens.client.handler;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.service.FeatureService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class UseHandlers {

    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();

    public static void register() {
        UseItemCallback.EVENT
                .register((player, level, hand) -> {
                    handleItemRightClick(player, hand);
                    return InteractionResult.PASS;
                });
    }

    private static void handleItemRightClick(final Player player, final InteractionHand hand) {
        if (!SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final ItemStack itemStack = player.getItemInHand(hand);
        for (final var feature : FEATURE_REGISTRY.getItemRightClickFeature()) {
            if (feature.isActive()) {
                feature.onItemRightClick(itemStack);
            }
        }
    }

}
