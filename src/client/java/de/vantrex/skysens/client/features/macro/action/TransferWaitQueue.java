package de.vantrex.skysens.client.features.macro.action;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.general.TransferCooldownQueueMode;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.skyblock.event.SkyblockQuitEvent;
import de.vantrex.skysens.client.skyblock.event.TransferCooldownDeniedEvent;
import de.vantrex.skysens.client.skyblock.event.TransferCooldownEndEvent;
import de.vantrex.skysens.client.skyblock.server.TransferCooldownTracker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds macro commands that Hypixel would refuse right after a server switch, and sends them when the transfer
 * cooldown is over. Only used when the "Wait for transfer cooldown" option is on. Client thread only.
 * <p>
 * A command that was sent anyway and got the cooldown message is queued once more ({@link TransferCooldownDeniedEvent}).
 */
public final class TransferWaitQueue {

    private record Held(String command, boolean retry) {
    }

    private record Sent(String command, long atMs, boolean retry) {
    }

    private static final Deque<Held> QUEUE = new ArrayDeque<>();
    private static @Nullable Sent lastSent;

    private TransferWaitQueue() {
    }

    public static void init() {
        EventListenerRegistry.REGISTRY.registerListener(event -> pump(), TransferCooldownEndEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(TransferWaitQueue::onDenied, TransferCooldownDeniedEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> clear(), SkyblockQuitEvent.class);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    /** Whether this command has to wait instead of being sent now. */
    static boolean shouldWait(String command) {
        return SkysensConfig.getInstance().generalCategory.waitForTransferCooldown
                && TransferCooldownTracker.getInstance().isActive()
                && TransferCooldownTracker.getInstance().isTransferLimited(command);
    }

    /** Holds a command until the cooldown is over, as the queue mode says. */
    static void enqueue(String command, boolean retry) {
        if (SkysensConfig.getInstance().generalCategory.transferCooldownQueueMode == TransferCooldownQueueMode.KEEP_NEWEST) {
            if (retry && !QUEUE.isEmpty()) {
                return; // what is held is newer than the command that was refused
            }
            QUEUE.clear();
        }
        QUEUE.addLast(new Held(command, retry));
    }

    /** Remembers a command a macro just sent, to re-queue it if Hypixel refuses it. */
    static void noteSent(String command, boolean retry) {
        lastSent = new Sent(command, System.currentTimeMillis(), retry);
    }

    public static int size() {
        return QUEUE.size();
    }

    public static void clear() {
        QUEUE.clear();
        lastSent = null;
    }

    /** Called every client tick (also retries after the global rate limit) and when the cooldown ends. */
    public static void pump() {
        if (QUEUE.isEmpty() || TransferCooldownTracker.getInstance().isActive() || MacroSender.rateLimitRemaining() > 0) {
            return;
        }
        final Held held = QUEUE.pollFirst();
        if (held != null) {
            MacroSender.sendNow(held.command(), held.retry());
        }
    }

    private static void onDenied(TransferCooldownDeniedEvent event) {
        final Sent sent = lastSent;
        if (sent == null || sent.retry() || event.getCommand() == null
                || System.currentTimeMillis() - sent.atMs() > TransferCooldownTracker.RECENT_SEND_MS
                || !SkysensConfig.getInstance().generalCategory.waitForTransferCooldown) {
            return;
        }
        lastSent = null;
        enqueue(sent.command(), true);
    }

}
