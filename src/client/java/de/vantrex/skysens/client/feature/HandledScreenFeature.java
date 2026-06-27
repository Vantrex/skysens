package de.vantrex.skysens.client.feature;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

public interface HandledScreenFeature extends Feature {

    Pattern inventoryName();

    default void draw$Head(GuiGraphicsExtractor context, Slot slot, CallbackInfo ci) {
    }

    default void mouseClicked$Head(MouseButtonEvent click, boolean doubled, AbstractContainerMenu menu, int screenX, int screenY, CallbackInfoReturnable<Boolean> cir) {
    }


    default boolean isInventoryName(String name) {
        return inventoryName().matcher(name).matches();
    }

    default boolean isInventory(Component text) {
        return text != null && this.isInventoryName(text.getString());
    }

    default boolean isPointOverSlot(Slot slot, double pointX, double pointY, int screenX, int screenY) {
        return this.isPointWithinBounds(slot.x, slot.y, 16, 16, pointX, pointY, screenX, screenY);
    }

    default boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY, int screenX, int screenY) {
        pointX -= (double) screenX;
        pointY -= (double) screenY;
        return pointX >= (double) (x - 1) && pointX < (double) (x + width + 1) && pointY >= (double) (y - 1) && pointY < (double) (y + height + 1);
    }

    default Slot getSlotAt(double mouseX, double mouseY, AbstractContainerMenu menu, int screenX, int screenY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && this.isPointOverSlot(slot, mouseX, mouseY, screenX, screenY)) {
                return slot;
            }
        }

        return null;
    }

}
