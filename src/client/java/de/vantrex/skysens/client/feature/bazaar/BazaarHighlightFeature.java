package de.vantrex.skysens.client.feature.bazaar;

import de.vantrex.skysens.client.feature.SkySensFeature;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@SkySensFeature
public class BazaarHighlightFeature extends AbstractBazaarFeature {

    @Override
    public Pattern inventoryName() {
        return Pattern.compile(".*Bazaar Orders");
    }

    @Override
    public void draw$Head(DrawContext context, Slot slot, CallbackInfo ci) {
        final ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return;
        }
        if (playerService.getCurrentPlayerName() == null) {
            return;
        }
        final OrderType orderType = super.getOrderType(stack);
        if (orderType == null) {
            return;
        }
        if (!this.shouldHighlight(orderType)) {
            return;
        }
        final LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return;
        }
        if (!super.isOwnPlayer(lore)) {
            return;
        }
        int x = slot.x;
        int y = slot.y;
        final int color = this.determineColor(lore, orderType);
        if (color == -1) return;
        context.fill(x, y, x + 16, y + 16, color);
    }

    private int determineColor(final LoreComponent lore, OrderType orderType) {
        boolean isFilled = super.isFilled(lore);
        return switch (orderType) {
            case BUY -> isFilled && bazaarCategory.highlightOwnBuyOrders.useDifferentColorFilledItems
                    ? bazaarCategory.highlightOwnBuyOrders.filledItemColor.getEffectiveColourRGB()
                    : bazaarCategory.highlightOwnBuyOrders.nonFilledHighlightColor.getEffectiveColourRGB();
            case SELL -> isFilled && bazaarCategory.highlightOwnSellOrders.useDifferentColorFilledItems
                    ? bazaarCategory.highlightOwnSellOrders.filledItemColor.getEffectiveColourRGB()
                    : bazaarCategory.highlightOwnSellOrders.nonFilledColor.getEffectiveColourRGB();
        };
    }

    private boolean shouldHighlight(AbstractBazaarFeature.OrderType orderType) {
        return switch (orderType) {
            case BUY -> bazaarCategory.highlightOwnBuyOrders.highlightOwnBuyOrders;
            case SELL -> bazaarCategory.highlightOwnSellOrders.highlightOwnSellOrders;
        };
    }

    @Override
    public boolean isActive() {
        return bazaarCategory.highlightOwnBuyOrders.highlightOwnBuyOrders
                || bazaarCategory.highlightOwnSellOrders.highlightOwnSellOrders;
    }

    private ChromaColour getHighlightColor(OrderType orderType) {
        return switch (orderType) {
            case BUY -> bazaarCategory.highlightOwnBuyOrders.nonFilledHighlightColor;
            case SELL -> bazaarCategory.highlightOwnSellOrders.nonFilledColor;
        };
    }

}
