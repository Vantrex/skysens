package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@UtilityClass
public class ChatUtil {

    /** The counter chat compacting mods append to repeated lines, e.g. "SLAYER QUEST COMPLETE! (2)". */
    private static final Pattern STACK_COUNTER = Pattern.compile("\\s+\\(\\d+\\)$");

    private static final String ANTI_SPAM_CHARS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /** Matches the suffix {@link #withAntiSpam(String)} appends, e.g. " [x7Qk]". */
    public static final String ANTI_SPAM_SUFFIX_REGEX = " \\[[A-Za-z0-9]{4}]";

    /**
     * The message with a random suffix like " [x7Qk]", so Hypixel doesn't block it as a repeat of an earlier message
     * ("You cannot say the same message twice!").
     */
    public @NotNull String withAntiSpam(@NotNull String message) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final StringBuilder suffix = new StringBuilder(" [");
        for (int i = 0; i < 4; i++) {
            suffix.append(ANTI_SPAM_CHARS.charAt(random.nextInt(ANTI_SPAM_CHARS.length())));
        }
        return message + suffix.append(']');
    }

    /** The line without leading/trailing spaces and without a compacting counter, ready for exact matching. */
    public @NotNull String normalize(@NotNull String line) {
        return STACK_COUNTER.matcher(line.trim()).replaceFirst("");
    }

    /** The text without formatting codes and surrounding spaces. */
    public @NotNull String strip(@NotNull String text) {
        final String stripped = ChatFormatting.stripFormatting(text);
        return (stripped == null ? text : stripped).trim();
    }

}
