package de.vantrex.skysens.client.chatcommand;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A command that other players can trigger by typing {@code !<name> [args...]} in a supported chat channel.
 * Register implementations in {@link ChatCommandRegistry}.
 */
public interface ChatCommand {

    /** The primary name, without prefix, e.g. "ping". */
    @NotNull String name();

    /** Alternative names, without prefix. */
    default @NotNull List<String> aliases() {
        return List.of();
    }

    /** Short description, shown by !help. */
    @NotNull String description();

    /** Argument usage without the command name, e.g. "[command]". Empty if the command takes no arguments. */
    default @NotNull String usage() {
        return "";
    }

    /** Whether the command is enabled in the config. */
    default boolean isEnabled() {
        return true;
    }

    /** The channels this command can be used in. */
    default @NotNull Set<ChatChannel> channels() {
        return EnumSet.allOf(ChatChannel.class);
    }

    /**
     * Whether answering this command starts the global reply cooldown. Commands that mostly don't
     * send a reply (or manage their own rate limit) return false.
     */
    default boolean usesCooldown() {
        return true;
    }

    /**
     * Runs the command on the client thread. Replies may be sent later from any thread via
     * {@link ChatCommandContext#reply(String)}.
     */
    void execute(@NotNull ChatCommandContext context);

}
