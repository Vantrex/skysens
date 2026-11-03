package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.platform.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public final class ScreenUtil {

    private ScreenUtil() {
    }

    /** Whether the screen is an inventory with the mouse over an item, as of the screen's last frame. */
    public static boolean isOverItem(@Nullable Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        final Slot slot = ((AbstractContainerScreenAccessor) screen).skysens$getHoveredSlot();
        return slot != null && slot.hasItem();
    }

}
