package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Decides whether a macro may fire right now. */
public final class MacroGate {

    /** BLOCKED: no macro; WORLD: no screen open; GUI: a container screen is open (only inGuis triggers apply). */
    public enum State { BLOCKED, WORLD, GUI }

    private MacroGate() {
    }

    public static boolean isSkyBlockOrBypassed() {
        return SkysensClient.getInstance().isOnSkyBlock()
                || (FabricLoader.getInstance().isDevelopmentEnvironment() && SkysensConfig.getInstance().devCategory.macroIgnoreSkyBlock);
    }

    public static State check() {
        final Minecraft mc = Minecraft.getInstance();
        if (!SkysensConfig.getInstance().macroCategory.enabled || mc.player == null || !isSkyBlockOrBypassed()) {
            return State.BLOCKED;
        }
        final Screen screen = mc.gui.screen();
        if (screen == null) {
            return State.WORLD;
        }
        // Chat, the editor, the config and every non-container screen block macros.
        if (!(screen instanceof AbstractContainerScreen<?>) || isTypingIntoTextField(screen)) {
            return State.BLOCKED;
        }
        return State.GUI;
    }

    private static boolean isTypingIntoTextField(Screen screen) {
        GuiEventListener focused = screen.getFocused();
        while (focused instanceof ContainerEventHandler container && container.getFocused() != null) {
            focused = container.getFocused();
        }
        return focused instanceof EditBox box && box.canConsumeInput();
    }

}
