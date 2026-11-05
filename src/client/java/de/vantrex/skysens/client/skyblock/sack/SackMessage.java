package de.vantrex.skysens.client.skyblock.sack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Hypixel's sack summary, sent every few seconds as "[Sacks] +179 items, -5 items. (Last 12s.)". The hover of
 * "+179 items" starts with "Added items:" and lists every added item ("+27 Gabagool Distillate (Combat Sack)"), the
 * hover of "-5 items" starts with "Removed items:" and lists every removed one ("-5 Festering Fly (Witch's Sack)").
 * The hovers are found by that first line (as Skyblocker and SkyHanni do), not by the text they sit on: Hypixel
 * splits "+179" and " items" into separate components.
 */
final class SackMessage {

    private static final Pattern SACKS_PATTERN = Pattern.compile("^\\[Sacks]\\s.*");
    /** The first line of the hover listing the added items. */
    private static final String ADDED_HOVER = "Added items";
    /** The first line of the hover listing the removed items. */
    private static final String REMOVED_HOVER = "Removed items";
    /** "(Last 12s.)" at the end of the message. */
    private static final Pattern WINDOW_PATTERN = Pattern.compile("\\(Last (\\d+)s\\.\\)");
    /** Used when the message doesn't say how long it sums up, a bit more than Hypixel's usual window. */
    private static final long DEFAULT_WINDOW_MS = 15_000;
    /** "+27 Gabagool Distillate (Combat Sack)", also tolerates a missing sack name. */
    private static final Pattern ADDED_LINE_PATTERN = Pattern.compile("^\\s*\\+([\\d,]+)x? (.+?)(?:\\s+\\(([^()]*)\\))?\\s*$");
    private static final Pattern REMOVED_LINE_PATTERN = Pattern.compile("^\\s*-([\\d,]+)x? (.+?)(?:\\s+\\(([^()]*)\\))?\\s*$");

    private SackMessage() {
    }

    /** Whether the message is a sack summary. */
    static boolean isSackMessage(@NotNull Component message) {
        final String text = ChatFormatting.stripFormatting(message.getString());
        return text != null && SACKS_PATTERN.matcher(text.trim()).matches();
    }

    /** How long the message sums up, from "(Last 12s.)". */
    static long windowMs(@NotNull Component message) {
        final String text = ChatFormatting.stripFormatting(message.getString());
        final Matcher matcher = text == null ? null : WINDOW_PATTERN.matcher(text);
        return matcher != null && matcher.find() ? Long.parseLong(matcher.group(1)) * 1000 : DEFAULT_WINDOW_MS;
    }

    /** The items that went into sacks, empty if the message isn't a sack summary or added nothing. */
    static @NotNull List<SackItem> added(@NotNull Component message) {
        return entries(message, ADDED_HOVER, ADDED_LINE_PATTERN);
    }

    /** The items that were taken out of sacks, empty if the message isn't a sack summary or removed nothing. */
    static @NotNull List<SackItem> removed(@NotNull Component message) {
        return entries(message, REMOVED_HOVER, REMOVED_LINE_PATTERN);
    }

    private static List<SackItem> entries(Component message, String hoverStart, Pattern line) {
        if (!isSackMessage(message)) {
            return List.of();
        }
        final String hoverText = findHover(message, hoverStart);
        if (hoverText == null) {
            return List.of();
        }
        final List<SackItem> entries = new ArrayList<>();
        for (final String hoverLine : hoverText.split("\\R")) {
            final Matcher matcher = line.matcher(hoverLine);
            if (matcher.matches()) {
                final String sack = matcher.group(3);
                entries.add(new SackItem(matcher.group(2).trim(), Integer.parseInt(matcher.group(1).replace(",", "")),
                        sack == null ? null : sack.trim()));
            }
        }
        return entries;
    }

    /** The stripped text of the first hover in the component tree that starts with {@code start}, or null. */
    private static @Nullable String findHover(Component component, String start) {
        if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowText showText) {
            final String text = ChatFormatting.stripFormatting(showText.value().getString());
            if (text != null && text.trim().startsWith(start)) {
                return text;
            }
        }
        for (final Component sibling : component.getSiblings()) {
            final String found = findHover(sibling, start);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
