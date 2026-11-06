package de.vantrex.skysens.client.features.macro.facts;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;

/** The screen that was open when the key was pressed (kept even if "close inventory first" closes it) and sneaking. */
public final class ScreenFacts {

    private final @Nullable Screen screen;

    public ScreenFacts(@Nullable Screen screen) {
        this.screen = screen;
    }

    public boolean isContainer() {
        return this.screen instanceof AbstractContainerScreen<?>;
    }

    /** The open container's title without formatting, null if no container screen was open. */
    public @Nullable String containerTitle() {
        if (!(this.screen instanceof AbstractContainerScreen<?> container)) {
            return null;
        }
        final String title = container.getTitle().getString();
        final String stripped = ChatFormatting.stripFormatting(title);
        return (stripped == null ? title : stripped).trim();
    }

    public boolean sneaking() {
        final Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isShiftKeyDown();
    }

}
