package de.vantrex.skysens.client.net.sync;

import java.time.Duration;

/**
 * Schedules {@link SyncTask}s on the existing {@code SkysensClient.SCHEDULER}.
 *
 * <p><strong>No new thread pool.</strong> Constraint 6 forbids per-feature
 * executors, and the shared pool has only 2 threads, so a task that blocks starves
 * everything else the mod schedules. Hence the hard per-request timeouts in
 * {@code SkysensApiClient}.
 *
 * <h2>Backoff</h2>
 *
 * On failure a task backs off exponentially from its interval up to
 * {@link #MAX_BACKOFF}, and resets on the first success. A version mismatch is not
 * a failure to back off from — it stops the scheduler outright for the session,
 * because retrying cannot change the answer.
 *
 * <h2>Startup</h2>
 *
 * Nothing is scheduled until the manifest has been fetched and found compatible,
 * and nothing at all is scheduled while the server is disabled in config — which
 * is the default. A fresh install makes zero network requests.
 *
 * <p><strong>Status: skeleton.</strong> Not wired into
 * {@code SkysensClient.onInitializeClient()}.
 */
public final class SyncScheduler {

    public static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

    private static SyncScheduler instance;

    private SyncScheduler() {
    }

    public static synchronized SyncScheduler getInstance() {
        if (instance == null) {
            instance = new SyncScheduler();
        }
        return instance;
    }

    /**
     * Fetches the manifest, checks it against
     * {@link de.vantrex.skysens.common.version.ApiVersion}, and only then starts the
     * registered tasks.
     */
    public void start() {
        // TODO(deferred): manifest fetch -> compatibility check -> scheduleAtFixedRate
        //                 each registered task on SkysensClient.SCHEDULER.
    }

    /** Cancels every scheduled task. Called when the user disables the server. */
    public void stop() {
        // TODO(deferred): cancel outstanding ScheduledFutures; do NOT shut down
        //                 SCHEDULER — it is shared with the rest of the mod.
    }

    public void register(final SyncTask task) {
        // TODO(deferred): hold the task and schedule it if already started.
    }
}
