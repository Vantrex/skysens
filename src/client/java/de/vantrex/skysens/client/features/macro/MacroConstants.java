package de.vantrex.skysens.client.features.macro;

/** Fixed macro timings. Deliberately not config options. */
public final class MacroConstants {

    /** Second press of a double tap must come within this window. */
    public static final long DOUBLE_TAP_MS = 250;
    /** A key held at least this long counts as a long hold. */
    public static final long LONG_HOLD_MS = 450;
    /** Minimum gap between two macro-sent commands/messages. */
    public static final long GLOBAL_RATE_LIMIT_MS = 500;
    /** Minimum gap between two "several branches matched" warnings for the same key. */
    public static final long WARNING_THROTTLE_MS = 10_000;

    private MacroConstants() {
    }

}
