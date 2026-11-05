package de.vantrex.skysens.client.features.macro.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** "Close inventory first": closes the open container or screen right before the action runs. */
public final class CloseScreenStep {

    private CloseScreenStep() {
    }

    public static void run() {
        final Minecraft mc = Minecraft.getInstance();
        final Screen screen = mc.gui.screen();
        if (screen == null) {
            return;
        }
        if (screen instanceof AbstractContainerScreen<?> && mc.player != null) {
            mc.player.closeContainer();
        } else {
            mc.gui.setScreen(null);
        }
    }

}
