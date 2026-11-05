package de.vantrex.skysens.client.skyblock.server;

import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import de.vantrex.skysens.client.skyblock.event.TransferCooldownDeniedEvent;
import de.vantrex.skysens.client.skyblock.event.TransferCooldownEndEvent;
import de.vantrex.skysens.client.skyblock.event.TransferCooldownStartEvent;
import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows when Hypixel blocks server-transfer commands (/warp, /hub, ...) after a server switch. Always active.
 * Client thread; the read API is safe from any thread.
 * <p>
 * A cooldown starts on every server switch ({@link #onServerSwitch}, called with the server name of each Hypixel Mod
 * API location packet) and lasts {@link #DEFAULT_COOLDOWN_MS} from the switch. The chat message "You may only use this
 * command after Ns on the server!" corrects it to N seconds after the last switch, and teaches the tracker that the
 * command sent just before is transfer-limited ({@link #isTransferLimited}); learned roots are saved in
 * {@link TransferCooldownRepository}. Fires {@link TransferCooldownStartEvent}, {@link TransferCooldownEndEvent}
 * (from a client tick check) and {@link TransferCooldownDeniedEvent}.
 * <p>
 * Outgoing commands are remembered through Fabric's {@code ClientSendMessageEvents.COMMAND}, which also sees commands
 * SkySens sends itself.
 * <p>
 * {@link #isActive()} is the cooldown state, not the usual always-true feature flag: this feature only listens to chat,
 * which is not gated by it, and ticks through its own Fabric callback.
 */
public final class TransferCooldownTracker implements GameMessageListeningFeature {

    @Getter
    private static final TransferCooldownTracker instance = new TransferCooldownTracker();

    /** How long after a server switch the transfer commands are blocked. */
    public static final long DEFAULT_COOLDOWN_MS = 4_000L;
    /** Used when the message says nothing newer than what is known: wait this long from now. */
    private static final long FALLBACK_MS = 1_000L;
    /** A command counts as the one that was refused if it was sent at most this long before the message. */
    public static final long RECENT_SEND_MS = 2_000L;

    /**
     * Command roots that are transfer-limited by default (first word, lower case, no slash); a two word entry such as
     * "p warp" matches both words. Extend this set to add more; further roots are learned at runtime.
     */
    public static final Set<String> DEFAULT_TRANSFER_LIMITED = Set.of(
            "warp", "is", "hub", "visit", "lobby", "l", "play", "server", "garden", "dungeon_hub",
            "bz", "ah",
            "p warp", "party warp");

    private static final Pattern DENIED = Pattern.compile("^You may only use this command after (\\d+)s on the server!(?: \\(\\d+\\))?$");

    private @Nullable String serverName;
    private volatile long lastSwitchMs;
    private volatile long endsAtMs;
    /** Whether a start event was fired and the end event is still due. */
    private boolean running;

    private @Nullable String lastSentCommand;
    private long lastSentAtMs;

    private TransferCooldownTracker() {
        ClientSendMessageEvents.COMMAND.register(this::onCommandSent);
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> this.serverName = null);
    }

    // ---------------------------------------------------------------- read API

    /** Whether the transfer cooldown is running. */
    @Override
    public boolean isActive() {
        return System.currentTimeMillis() < this.endsAtMs;
    }

    /** Milliseconds left of the cooldown, 0 if there is none. */
    public long remainingMs() {
        return Math.max(0, this.endsAtMs - System.currentTimeMillis());
    }

    /** Whether a command (with or without "/") is known to be blocked by the transfer cooldown. */
    public boolean isTransferLimited(@NotNull String command) {
        final String root = commandRoot(command);
        if (root.isEmpty()) {
            return false;
        }
        final String firstWord = root.contains(" ") ? root.substring(0, root.indexOf(' ')) : root;
        return DEFAULT_TRANSFER_LIMITED.contains(root) || DEFAULT_TRANSFER_LIMITED.contains(firstWord)
                || this.learned(root);
    }

    /** The command roots learned from Hypixel's message so far. */
    public @NotNull List<String> learnedCommands() {
        final TransferCooldownRepository repository = RepositoryRegistry.TRANSFER_COOLDOWN_REPOSITORY;
        if (repository == null) {
            return List.of();
        }
        synchronized (repository) {
            return Collections.unmodifiableList(List.copyOf(repository.learnedCommands));
        }
    }

    private boolean learned(String root) {
        return this.learnedCommands().contains(root);
    }

    // ---------------------------------------------------------------- server switch and sending

    /** Called with the server name of every Mod API location packet, on the client thread. */
    public void onServerSwitch(@Nullable String newServerName) {
        if (newServerName == null || newServerName.equals(this.serverName)) {
            return;
        }
        this.serverName = newServerName;
        final long now = System.currentTimeMillis();
        this.lastSwitchMs = now;
        this.start(now + DEFAULT_COOLDOWN_MS);
    }

    private void onCommandSent(String command) {
        this.lastSentCommand = command;
        this.lastSentAtMs = System.currentTimeMillis();
    }

    // ---------------------------------------------------------------- chat

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }
        final Matcher matcher = DENIED.matcher(message.getString().trim());
        if (!matcher.matches()) {
            return;
        }
        final long now = System.currentTimeMillis();
        final long candidate = this.lastSwitchMs + Long.parseLong(matcher.group(1)) * 1000L;
        if (candidate > this.endsAtMs && candidate > now) {
            this.start(candidate);
        } else if (this.endsAtMs < now + FALLBACK_MS) {
            this.start(now + FALLBACK_MS);
        }
        String command = null;
        final String sent = this.lastSentCommand;
        if (sent != null && now - this.lastSentAtMs <= RECENT_SEND_MS) {
            command = sent;
            this.learn(sent);
        }
        EventListenerRegistry.REGISTRY.fireEvent(new TransferCooldownDeniedEvent(command));
    }

    private void learn(String command) {
        final String root = commandRoot(command);
        if (root.isEmpty() || root.equals("p") || root.equals("party") || this.isTransferLimited(root)) {
            return; // "p" alone is not a transfer command, only "p warp"
        }
        final TransferCooldownRepository repository = RepositoryRegistry.TRANSFER_COOLDOWN_REPOSITORY;
        if (repository == null) {
            return;
        }
        synchronized (repository) {
            repository.learnedCommands.add(root);
        }
        RepositoryRegistry.saveRepository(repository);
    }

    /** "/Warp  hub" becomes "warp"; "/p warp x" and "/party warp x" keep both words. */
    static String commandRoot(String command) {
        final String trimmed = command.trim().toLowerCase(Locale.ROOT);
        final String stripped = trimmed.startsWith("/") ? trimmed.substring(1).trim() : trimmed;
        final String[] words = stripped.split("\\s+");
        if (words.length >= 2 && (words[0].equals("p") || words[0].equals("party"))) {
            return words[0] + " " + words[1];
        }
        return words[0];
    }

    // ---------------------------------------------------------------- state

    private void start(long endsAt) {
        this.endsAtMs = endsAt;
        this.running = true;
        EventListenerRegistry.REGISTRY.fireEvent(new TransferCooldownStartEvent(endsAt));
    }

    private void tick() {
        if (this.running && System.currentTimeMillis() >= this.endsAtMs) {
            this.running = false;
            EventListenerRegistry.REGISTRY.fireEvent(new TransferCooldownEndEvent());
        }
    }

}
