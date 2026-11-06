package de.vantrex.skysens.client.core.feature;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sees raw keyboard and mouse button events at the head of {@code KeyboardHandler.keyPress} and
 * {@code MouseHandler.onButton}, before screens and key mappings. Cancelling {@code ci} swallows the event.
 *
 * <p>Called on the client thread, unconditionally for active features: check the SkyBlock/screen state yourself.
 * {@code action} is the GLFW action: 1 press, 0 release, 2 repeat.
 */
public interface RawInputFeature extends Feature {

    void keyPress$Head(KeyEvent event, int action, CallbackInfo ci);

    void mouseButton$Head(MouseButtonInfo button, int action, CallbackInfo ci);

}
