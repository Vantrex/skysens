package de.vantrex.skysens.client.net.sync;

import java.time.Duration;

/**
 * One background refresh job.
 *
 * <p>Implementations must be entirely side-effect-free with respect to game state:
 * they refresh {@code client/net/cache} and nothing else. Anything that must touch
 * the client goes through {@code Minecraft.getInstance().execute(...)}.
 */
public interface SyncTask {

    /** Stable id, used for logging and for the remote feature-flag kill-switch. */
    String id();

    /** How often this task wants to run when everything is healthy. */
    Duration interval();

    /**
     * Performs one refresh. Must never throw and must never block longer than the
     * request timeout; it runs on the shared 2-thread {@code SkysensClient.SCHEDULER}
     * that the rest of the mod also uses.
     */
    void runOnce();
}
