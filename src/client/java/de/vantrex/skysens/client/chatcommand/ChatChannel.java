package de.vantrex.skysens.client.chatcommand;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * A Hypixel chat channel that chat commands can be received from and answered in.
 * Add a constant here (prefix + reply command) to support another channel.
 */
@Getter
public enum ChatChannel {

    COOP("Co-op", "cchat"),
    PARTY("Party", "pc");

    /** The text before " > " in a channel message, e.g. "Co-op". */
    private final String prefix;
    /** The command (without slash) used to send a message to this channel. */
    private final String replyCommand;
    /**
     * Matches a formatting-stripped message, e.g. "Co-op > [MVP++] Vantrex: !ping".
     * Group 1 is the sender name, group 2 the message. Allows an optional symbol before the
     * rank (e.g. ironman) and an optional symbol after the name.
     */
    private final Pattern pattern;

    ChatChannel(@NotNull String prefix, @NotNull String replyCommand) {
        this.prefix = prefix;
        this.replyCommand = replyCommand;
        this.pattern = Pattern.compile("^" + Pattern.quote(prefix)
                + " > (?:[^\\w\\s\\[]+ )?(?:\\[[^]]+] )?(\\w{1,16})(?: [^\\w\\s:]+)?: (.*)$");
    }

}
