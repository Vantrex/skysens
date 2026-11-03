package de.vantrex.skysens.client.platform.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    /** The slot under the mouse, set while the screen is drawn, so from its last frame during the HUD. */
    @Accessor("hoveredSlot")
    @Nullable Slot skysens$getHoveredSlot();

}
