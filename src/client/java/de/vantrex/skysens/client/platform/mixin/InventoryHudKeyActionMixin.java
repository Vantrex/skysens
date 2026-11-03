package de.vantrex.skysens.client.platform.mixin;

import de.vantrex.skysens.client.features.actionbar.ActionBarLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inventory HUD+'s key bindings (show/hide the inventory HUD, toggle all, ...). Each method returns true when it ran
 * its action, which is when the action bar layout has to be checked again. Only applied if Inventory HUD+ is
 * installed; {@code require = 0} so a changed Inventory HUD+ version can't stop the game from starting.
 */
@Pseudo
@Mixin(targets = "dlovin.inventoryhud.keybinds.KeyAction", remap = false)
public abstract class InventoryHudKeyActionMixin {

    @Inject(method = {"consume", "keyDown", "mouseDown"}, at = @At("RETURN"), require = 0)
    private void skysens$onKeyAction(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ActionBarLayout.getInstance().markDirty();
        }
    }

}
