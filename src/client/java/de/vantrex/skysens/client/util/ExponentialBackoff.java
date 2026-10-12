package de.vantrex.skysens.client.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential backoff for retrying failing work such as API requests: every failure in a row doubles the wait,
 * starting at {@code baseDelayMs} and capped at {@code maxDelayMs}, with up to 10% random jitter so several
 * clients don't retry in lockstep. A success resets it. Thread-safe.
 */
public final class ExponentialBackoff {

    private static final double JITTER = 0.1;

    private final long baseDelayMs;
    private final long maxDelayMs;
    private int failures;
    private long retryAt;

    public ExponentialBackoff(long baseDelayMs, long maxDelayMs) {
        if (baseDelayMs <= 0 || maxDelayMs < baseDelayMs) {
            throw new IllegalArgumentException("Need 0 < baseDelayMs <= maxDelayMs");
        }
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    /** Whether the backoff after the last failure is over, so the work may be tried again. */
    public synchronized boolean canAttempt() {
        return System.currentTimeMillis() >= this.retryAt;
    }

    /** Records a failure and returns the delay in ms until the next attempt is allowed. */
    public synchronized long onFailure() {
        // 2^30 is far beyond any sensible cap, stop growing the exponent there to avoid overflow
        this.failures = Math.min(this.failures + 1, 30);
        final long delay = Math.min(this.maxDelayMs, this.baseDelayMs << (this.failures - 1));
        final long jittered = delay + (long) (delay * JITTER * ThreadLocalRandom.current().nextDouble());
        this.retryAt = System.currentTimeMillis() + jittered;
        return jittered;
    }

    /** Records a success, the next failure starts at the base delay again. */
    public synchronized void onSuccess() {
        this.failures = 0;
        this.retryAt = 0;
    }

    /** Failures in a row since the last success. */
    public synchronized int failures() {
        return this.failures;
    }

}
