package de.vantrex.skysens.client.features.macro.action;

import de.vantrex.skysens.client.features.macro.MacroConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Sends commands and chat for macros, with a global rate limit, sanitising and a length cap. */
public final class MacroSender {

    /** Hypixel's chat length limit. Longer text is refused, never truncated. */
    public static final int MAX_LENGTH = 256;

    private static long nextAllowedAt;

    private MacroSender() {
    }

    /** Strips colour codes and newlines and collapses whitespace. */
    public static String sanitize(String text) {
        return text.replaceAll("§.?", "").replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
    }

    /** Milliseconds until the next server-bound send is allowed, 0 when allowed now. */
    public static long rateLimitRemaining() {
        return Math.max(0, nextAllowedAt - System.currentTimeMillis());
    }

    public static ActionOutcome sendCommand(String raw) {
        String command = sanitize(raw);
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank()) {
            return ActionOutcome.failed("empty command");
        }
        return send(command, true);
    }

    public static ActionOutcome sendChat(String raw) {
        final String message = sanitize(raw);
        if (message.isEmpty()) {
            return ActionOutcome.failed("empty message");
        }
        if (message.startsWith("/")) {
            return ActionOutcome.failed("chat messages must not start with /");
        }
        return send(message, false);
    }

    private static ActionOutcome send(String text, boolean command) {
        if (text.length() > MAX_LENGTH) {
            return ActionOutcome.failed("too long (" + text.length() + " > " + MAX_LENGTH + ")");
        }
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return ActionOutcome.failed("not connected");
        }
        if (command) {
            connection.sendCommand(text);
        } else {
            connection.sendChat(text);
        }
        nextAllowedAt = System.currentTimeMillis() + MacroConstants.GLOBAL_RATE_LIMIT_MS;
        return ActionOutcome.ran();
    }

}
