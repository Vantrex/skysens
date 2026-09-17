package de.vantrex.skysens.client.feature.bazaar;

import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

@SkySensFeature
public class BazaarDisableClickFeature extends AbstractBazaarFeature {

    @Override
    public Pattern inventoryName() {
        return Pattern.compile(".*Bazaar Orders");
    }

    @Override
    public void mouseClicked$Head(MouseButtonEvent click, boolean doubled, AbstractContainerMenu menu, int screenX, int screenY, CallbackInfoReturnable<Boolean> cir) {

        Slot slot = this.getSlotAt(click.x(), click.y(), menu, screenX, screenY);
        if (slot == null) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        BazaarHighlightFeature.OrderType orderType = this.getOrderType(stack);
        if (orderType == null) {
            return;
        }
        final boolean isOwnPlayer = this.isOwnPlayer(stack.get(DataComponents.LORE));
        if (isOwnPlayer) {
            return;
        }
        // if user is holding STRG this is bypassed
        if (click.hasControlDown()) {
            return;
        }
        ClientUtil.sendMessage("§4Skysens protected a non-self owned bazaar listing to be claimed! §cClick with CTRL to bypass.");
        ClientUtil.playSound(SoundEvents.ANVIL_HIT, 1.0F, 1.0F);
        cir.setReturnValue(false);
    }
    

    @Override
    public boolean isActive() {
        return super.bazaarCategory.disableClicksOnNonSelfOwned;
    }
}
