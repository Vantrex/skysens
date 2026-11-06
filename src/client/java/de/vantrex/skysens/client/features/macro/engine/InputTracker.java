package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.features.macro.MacroConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Press state for raw input.
 * <ul>
 *   <li>Consumed keys: the physical keys whose press was swallowed, so their repeats are swallowed too (otherwise
 *   vanilla would see a repeat without a press). Releases clear it.</li>
 *   <li>Pending presses: presses of a chord that has double tap or long hold triggers. They are not resolved at the
 *   press; they pass through to vanilla and resolve later (see {@link #evaluate}), driven by releases and the tick.</li>
 * </ul>
 */
public final class InputTracker {

    /** A press waiting for a double tap window or a hold threshold. */
    public static final class Pending {
        private final TriggerIndex.BindingGroup group;
        private final InputDevice device;
        private final int code;
        private final long downAt;
        private boolean down = true;
        private boolean done;

        private Pending(TriggerIndex.BindingGroup group, InputDevice device, int code, long downAt) {
            this.group = group;
            this.device = device;
            this.code = code;
            this.downAt = downAt;
        }

        public TriggerIndex.BindingGroup group() {
            return this.group;
        }
    }

    private final Set<Long> consumedHeld = new HashSet<>();
    private final List<Pending> pending = new ArrayList<>();

    private static long key(InputDevice device, int code) {
        return ((long) device.ordinal() << 32) | (code & 0xFFFFFFFFL);
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    // ---- consumed keys ----

    public void consume(InputDevice device, int code) {
        this.consumedHeld.add(key(device, code));
    }

    public boolean isConsumed(InputDevice device, int code) {
        return this.consumedHeld.contains(key(device, code));
    }

    // ---- pending presses ----

    /**
     * A press of a deferred chord. If it is the second press of a pending first press inside the double tap window,
     * that first press is finished and the result is DOUBLE_TAP (the caller resolves it synchronously). Otherwise it
     * becomes a new pending press and the result is NONE.
     */
    public enum PressResult { NONE, DOUBLE_TAP }

    public PressResult press(TriggerIndex.BindingGroup group, InputDevice device, int code, BiConsumer<Pending, PressMode> resolver) {
        final long now = now();
        // finish what is overdue first, so a late second press doesn't count as a double tap
        this.tick(resolver);
        if (group.hasDoubleTap()) {
            for (Pending p : this.pending) {
                if (!p.done && !p.down && p.group.chord().equals(group.chord()) && now - p.downAt <= MacroConstants.DOUBLE_TAP_MS) {
                    p.done = true;
                    this.pending.remove(p);
                    return PressResult.DOUBLE_TAP;
                }
            }
        }
        this.pending.add(new Pending(group, device, code, now));
        return PressResult.NONE;
    }

    /** The key went up: a pending press of it may resolve now. */
    public void release(InputDevice device, int code, BiConsumer<Pending, PressMode> resolver) {
        this.consumedHeld.remove(key(device, code));
        for (Pending p : new ArrayList<>(this.pending)) {
            if (p.down && p.device == device && p.code == code) {
                p.down = false;
                this.evaluate(p, now(), resolver);
            }
        }
        this.pending.removeIf(p -> p.done);
    }

    /** Resolves pending presses whose window or threshold has passed. Called every client tick. */
    public void tick(BiConsumer<Pending, PressMode> resolver) {
        if (this.pending.isEmpty()) {
            return;
        }
        final long now = now();
        for (Pending p : new ArrayList<>(this.pending)) {
            this.evaluate(p, now, resolver);
        }
        for (Iterator<Pending> it = this.pending.iterator(); it.hasNext(); ) {
            if (it.next().done) {
                it.remove();
            }
        }
    }

    /**
     * <ul>
     *   <li>Held for the long hold time: LONG_HOLD, nothing else follows.</li>
     *   <li>Released: with a double tap trigger on the chord wait out the window, then SINGLE; otherwise SINGLE now.</li>
     *   <li>Still down, window over, no long hold trigger to wait for: SINGLE.</li>
     * </ul>
     * SINGLE and LONG_HOLD here never consume: the press already reached vanilla.
     */
    private void evaluate(Pending p, long now, BiConsumer<Pending, PressMode> resolver) {
        if (p.done) {
            return;
        }
        final TriggerIndex.BindingGroup group = p.group;
        if (p.down && group.hasLongHold() && now - p.downAt >= MacroConstants.LONG_HOLD_MS) {
            p.done = true;
            resolver.accept(p, PressMode.LONG_HOLD);
            return;
        }
        final boolean windowOver = now - p.downAt > MacroConstants.DOUBLE_TAP_MS;
        if (!p.down) {
            if (!group.hasDoubleTap() || windowOver) {
                p.done = true;
                resolver.accept(p, PressMode.SINGLE);
            }
        } else if (group.hasDoubleTap() && !group.hasLongHold() && windowOver) {
            p.done = true;
            resolver.accept(p, PressMode.SINGLE);
        }
    }

}
