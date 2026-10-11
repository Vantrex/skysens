package de.vantrex.skysens.client.chatcommand;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * @param channel the channel the command was sent in; replies go back to it
 * @param sender  name of the player who sent the command
 * @param label   the name or alias that was typed, lower case
 * @param args    the whitespace separated arguments after the label
 */
public record ChatCommandContext(@NotNull ChatChannel channel,
                                 @NotNull String sender,
                                 @NotNull String label,
                                 @NotNull List<String> args,
                                 @NotNull Consumer<String> replySender) {

    /** Sends a message to the channel the command came from. Safe to call from any thread. */
    public void reply(@NotNull String message) {
        this.replySender.accept(message);
    }

    public boolean hasArgs() {
        return !this.args.isEmpty();
    }

    public @NotNull String arg(int index) {
        return this.args.get(index);
    }

}
