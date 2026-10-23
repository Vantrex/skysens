package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@UtilityClass
public class ChatUtil {

    /** The counter chat compacting mods append to repeated lines, e.g. "SLAYER QUEST COMPLETE! (2)". */
    private static final Pattern STACK_COUNTER = Pattern.compile("\\s+\\(\\d+\\)$");

    /** The line without leading/trailing spaces and without a compacting counter, ready for exact matching. */
    public @NotNull String normalize(@NotNull String line) {
        return STACK_COUNTER.matcher(line.trim()).replaceFirst("");
    }

}
