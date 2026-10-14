package de.vantrex.skysens.client.stats;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import static de.vantrex.skysens.client.stats.InternalStatsProvider.TPS_SAMPLES;

@Setter
@Getter
public final class ClientStats {

    /** TPS state, only touched on the client thread **/
    private final double[] tpsSamples = new double[TPS_SAMPLES];
    private int tpsIndex;
    private int tpsCount;

    /** Holds the users last ping **/
    private volatile int lastPing = -1;

    /** Holds the current session join time; Resets once the player leaves skyblock **/
    @Setter(AccessLevel.NONE)
    private @Nullable Long currentSessionJoin = null;
    /** Holds the playtime of a session after the user disconnects from skyblock **/
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private long accumulatedSessionPlaytime = 0;

    public void startSession() {
        this.currentSessionJoin = System.currentTimeMillis();
    }

    public void stopSession() {
        final long currentSessionPlaytime = this.getCurrentSessionPlaytime();
        this.currentSessionJoin = null;
        this.accumulatedSessionPlaytime = this.accumulatedSessionPlaytime + currentSessionPlaytime;
    }


    /**
     * @return the total session playtime since the start of the client
     */
    public long getAccumulatedSessionPlaytime() {
        return this.getCurrentSessionPlaytime() + this.accumulatedSessionPlaytime;
    }

    /**
     * @return the current session playtime since joining skyblock
     */
    public long getCurrentSessionPlaytime() {
        if (this.currentSessionJoin == null) {
            return 0;
        }
        return System.currentTimeMillis() - this.currentSessionJoin;
    }

}
