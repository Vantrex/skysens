package de.vantrex.skysens.client.feature.bazaar;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.bazaar.BazaarCategory;
import de.vantrex.skysens.client.feature.HandledScreenFeature;
import de.vantrex.skysens.client.service.PlayerService;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public abstract class AbstractBazaarFeature implements HandledScreenFeature {

    protected final BazaarCategory bazaarCategory = SkysensConfig.getInstance().bazaarCategory;
    protected final PlayerService playerService = PlayerService.getInstance();

    protected final OrderType getOrderType(ItemStack stack) {
        final String name = stack.getName().getString();
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

    protected boolean isFilled(LoreComponent lore) {
        if (lore.lines().size() < 4) {
            return false;
        }
        final Text filledLine = lore.lines().get(3);
        return filledLine.getString().endsWith("100%!");
    }

    protected boolean isOwnPlayer(LoreComponent lore) {
        for (final Text line : lore.lines()) {
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
