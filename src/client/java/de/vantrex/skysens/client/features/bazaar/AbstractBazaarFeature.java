package de.vantrex.skysens.client.features.bazaar;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.bazaar.config.BazaarCategory;
import de.vantrex.skysens.client.core.feature.HandledScreenFeature;
import de.vantrex.skysens.client.skyblock.player.PlayerService;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public abstract class AbstractBazaarFeature implements HandledScreenFeature {

    protected final BazaarCategory bazaarCategory = SkysensConfig.getInstance().bazaarCategory;
    protected final PlayerService playerService = PlayerService.getInstance();

    protected final OrderType getOrderType(ItemStack stack) {
        final String name = stack.getHoverName().getString();
        final String[] parts = name.split(" ");
        if (parts.length < 1) {
            return null;
        }
        return switch (parts[0]) {
            case "BUY" -> OrderType.BUY;
            case "SELL" -> OrderType.SELL;
            default -> null;
        };
    }

    protected boolean isFilled(ItemLore lore) {
        if (lore.lines().size() < 4) {
            return false;
        }
        final Component filledLine = lore.lines().get(3);
        return filledLine.getString().endsWith("100%!");
    }

    protected boolean isOwnPlayer(ItemLore lore) {
        for (final Component line : lore.lines()) {
            final var text = line.getString();
            if (!text.startsWith("By:")) {
                continue;
            }
            if (text.endsWith(playerService.getCurrentPlayerName())) {
                return true;
            }
        }
        return false;
    }

    protected enum OrderType {
        BUY, SELL
    }

}
