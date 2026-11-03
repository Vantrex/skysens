package de.vantrex.skysens.client.ui.render;

import net.minecraft.util.Util;

/**
 * A float that eases exponentially toward a target, using real time (not ticks).
 * {@link #value()} advances the animation implicitly, so calling {@link #update()} is optional.
 */
public final class Animated {

    private float value;
    private float target;
    private final float halfLifeMs;
    private long lastMillis = -1;

    /**
     * @param initial    starting value
     * @param durationMs approximate time to settle (about 95 percent) in milliseconds
     */
    public Animated(float initial, float durationMs) {
        this.value = initial;
        this.target = initial;
        this.halfLifeMs = Math.max(1f, durationMs / 4.3f);
    }

    /** Creates an animation that settles in about 150ms. */
    public Animated(float initial) {
        this(initial, 150f);
    }

    /** Current eased value (advances the animation to the current time). */
    public float value() {
        update();
        return value;
    }

    /** Sets the target to ease toward. */
    public void target(float t) {
        update();
        this.target = t;
    }

    public float targetValue() {
        return target;
    }

    /** Jumps immediately to the given value (no animation). */
    public void snap(float v) {
        this.value = v;
        this.target = v;
        this.lastMillis = Util.getMillis();
    }

    /** Advances the animation using the real time elapsed since the last update. */
    public void update() {
        long now = Util.getMillis();
        if (lastMillis < 0) {
            lastMillis = now;
            return;
        }
        long dt = now - lastMillis;
        lastMillis = now;
        if (dt <= 0 || value == target) {
            return;
        }
        float k = 1f - (float) Math.pow(0.5, dt / halfLifeMs);
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.001f) {
            value = target;
        }
    }

    /** True while still moving toward the target. */
    public boolean isAnimating() {
        return value != target;
    }
}
