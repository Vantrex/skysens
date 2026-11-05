package de.vantrex.skysens.client.features.macro;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.RawInputFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.features.macro.engine.InputDevice;
import de.vantrex.skysens.client.features.macro.engine.MacroEngine;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks raw input into the macro engine. */
@SkySensFeature
public class MacroFeature implements RawInputFeature {

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().macroCategory.enabled;
    }

    @Override
    public void keyPress$Head(KeyEvent event, int action, CallbackInfo ci) {
        MacroEngine.getInstance().onInput(InputDevice.KEYBOARD, event.key(), event, action, ci);
    }

    @Override
    public void mouseButton$Head(MouseButtonInfo button, int action, CallbackInfo ci) {
        MacroEngine.getInstance().onInput(InputDevice.MOUSE, button.button(), button, action, ci);
    }

}
