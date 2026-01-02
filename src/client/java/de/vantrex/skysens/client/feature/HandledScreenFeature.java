package de.vantrex.skysens.client.feature;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

public interface HandledScreenFeature extends Feature {

    Pattern inventoryName();

    default void draw$Head(DrawContext context, Slot slot, CallbackInfo ci) {
    }

    default void mouseClicked$Head(Click click, boolean doubled, ScreenHandler screenHandler, int screenX, int screenY, CallbackInfoReturnable<Boolean> cir) {
    }


    default boolean isInventoryName(String name) {
        return inventoryName().matcher(name).matches();
    }

    default boolean isInventory(Text text) {
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

    default Slot getSlotAt(double mouseX, double mouseY, ScreenHandler handler, int screenX, int screenY) {
        for (Slot slot : handler.slots) {
            if (slot.isEnabled() && this.isPointOverSlot(slot, mouseX, mouseY, screenX, screenY)) {
                return slot;
            }
        }

        return null;
    }

}
