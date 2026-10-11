package de.vantrex.skysens.client.profit;

import com.google.gson.annotations.Expose;

/**
 * Profit of one tracker (overall or an activity) over some period. Uptime works like SkyHanni's trackers:
 * the time between two gains counts, but at most the AFK timeout (after that the stopwatch is paused).
 */
public class ProfitStats {

    @Expose
    public double profit;
    @Expose
    public long uptimeMs;
    @Expose
    public long events;
    @Expose
    public long lastUpdateMs;

    public void add(double amount, long now, long afkTimeoutMs) {
        if (this.lastUpdateMs > 0) {
            final long gap = now - this.lastUpdateMs;
            if (gap > 0) {
                this.uptimeMs += Math.min(gap, afkTimeoutMs);
            }
        }
        this.lastUpdateMs = now;
        this.profit += amount;
        this.events++;
    }

    /** Profit per hour, or NaN while there is not enough uptime for a meaningful rate. */
    public double perHour() {
        // under a minute the rate is mostly noise
        return this.uptimeMs < 60_000 ? Double.NaN : this.profit / (this.uptimeMs / 3_600_000.0);
    }

    /** A copy including the time since the last gain (up to the AFK timeout), like a running stopwatch. */
    public ProfitStats withLiveUptime(long now, long afkTimeoutMs) {
        final ProfitStats copy = new ProfitStats();
        copy.profit = this.profit;
        copy.events = this.events;
        copy.lastUpdateMs = this.lastUpdateMs;
        copy.uptimeMs = this.uptimeMs + (this.lastUpdateMs > 0 ? Math.max(0, Math.min(now - this.lastUpdateMs, afkTimeoutMs)) : 0);
        return copy;
    }

    public void reset() {
        this.profit = 0;
        this.uptimeMs = 0;
        this.events = 0;
        this.lastUpdateMs = 0;
    }

}
