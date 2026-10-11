package de.vantrex.skysens.client.profit;

import com.google.gson.annotations.Expose;

/**
 * Profit of one tracker (overall or an activity) over some period. Uptime works like SkyHanni's trackers:
 * time between two gains counts, unless the gap is longer than the AFK timeout.
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
            if (gap > 0 && gap <= afkTimeoutMs) {
                this.uptimeMs += gap;
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

    public void reset() {
        this.profit = 0;
        this.uptimeMs = 0;
        this.events = 0;
        this.lastUpdateMs = 0;
    }

}
