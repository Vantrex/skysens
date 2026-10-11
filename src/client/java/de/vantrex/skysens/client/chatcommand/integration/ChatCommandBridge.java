package de.vantrex.skysens.client.chatcommand.integration;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import org.jetbrains.annotations.NotNull;

/** Tells whether another mod already answers a chat command, so SkySens can stay quiet. */
public interface ChatCommandBridge {

    /** Display name, used for debug output. */
    @NotNull String name();

    /**
     * @param label  the typed command name, lower case and without prefix
     * @param sender the player who sent the command
     */
    boolean handles(@NotNull ChatChannel channel, @NotNull String label, @NotNull String sender);

}
