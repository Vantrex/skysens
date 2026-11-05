package de.vantrex.skysens.client.features.macro.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * Press state for raw input. SINGLE presses only for now: it remembers which physical keys had their press consumed,
 * so their repeats are swallowed too (otherwise vanilla would see a repeat without a press). Releases clear it.
 */
public final class InputTracker {

    private final Set<Long> consumedHeld = new HashSet<>();

    private static long key(InputDevice device, int code) {
        return ((long) device.ordinal() << 32) | (code & 0xFFFFFFFFL);
    }

    public void consume(InputDevice device, int code) {
        this.consumedHeld.add(key(device, code));
    }

    public boolean isConsumed(InputDevice device, int code) {
        return this.consumedHeld.contains(key(device, code));
    }

    public void release(InputDevice device, int code) {
        this.consumedHeld.remove(key(device, code));
    }

}
