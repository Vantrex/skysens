package de.vantrex.skysens.client.feature.bazaar;

import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

@SkySensFeature
public class BazaarDisableClickFeature extends AbstractBazaarFeature {

    @Override
    public Pattern inventoryName() {
        return Pattern.compile(".*Bazaar Orders");
    }

    @Override
    public void mouseClicked$Head(Click click, boolean doubled, ScreenHandler screenHandler, int screenX, int screenY, CallbackInfoReturnable<Boolean> cir) {

        Slot slot = this.getSlotAt(click.x(), click.y(), screenHandler, screenX, screenY);
        if (slot == null) {
            return;
        }
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return;
        }
        BazaarHighlightFeature.OrderType orderType = this.getOrderType(stack);
        if (orderType == null) {
            return;
        }
        final boolean isOwnPlayer = this.isOwnPlayer(stack.get(DataComponentTypes.LORE));
        if (isOwnPlayer) {
            return;
        }
        // if user is holding STRG this is bypassed
        if (click.hasCtrl()) {
            return;
        }
        ClientUtil.sendMessage("§4Skysens protected a non-self owned bazaar listing to be claimed! §cClick with CTRL to bypass.");
        ClientUtil.playSound(SoundEvents.BLOCK_ANVIL_HIT, 1.0F, 1.0F);
        cir.setReturnValue(false);
    }
    

    @Override
    public boolean isActive() {
        return super.bazaarCategory.disableClicksOnNonSelfOwned;
    }
}
