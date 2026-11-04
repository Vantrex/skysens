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
 * "+179 items" lists every added item ("+27 Gabagool Distillate (Combat Sack)"), the hover of "-5 items" every
 * removed one ("-5 Festering Fly (Witch's Sack)").
 */
final class SackMessage {

    private static final Pattern SACKS_PATTERN = Pattern.compile("^\\[Sacks]\\s.*");
    /** The "+179 items" or "-5 items" part of the message, whose hover lists the items. */
    private static final Pattern ADDED_PATTERN = Pattern.compile("^\\+[\\d,]+ items?\\b.*");
    /** "(Last 12s.)" at the end of the message. */
    private static final Pattern WINDOW_PATTERN = Pattern.compile("\\(Last (\\d+)s\\.\\)");
    /** Used when the message doesn't say how long it sums up, a bit more than Hypixel's usual window. */
    private static final long DEFAULT_WINDOW_MS = 15_000;
    private static final Pattern REMOVED_PATTERN = Pattern.compile("^-[\\d,]+ items?\\b.*");
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
        return entries(message, ADDED_PATTERN, ADDED_LINE_PATTERN);
    }

    /** The items that were taken out of sacks, empty if the message isn't a sack summary or removed nothing. */
    static @NotNull List<SackItem> removed(@NotNull Component message) {
        return entries(message, REMOVED_PATTERN, REMOVED_LINE_PATTERN);
    }

    private static List<SackItem> entries(Component message, Pattern part, Pattern line) {
        if (!isSackMessage(message)) {
            return List.of();
        }
        final Component hover = findHover(message, null, part);
        final String hoverText = hover == null ? null : ChatFormatting.stripFormatting(hover.getString());
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

    /** The hover text of the component matching {@code part}, searching the siblings and inheriting the parent's hover. */
    private static @Nullable Component findHover(Component component, @Nullable Component inherited, Pattern part) {
        Component hover = inherited;
        if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowText showText) {
            hover = showText.value();
        }
        final String own = ChatFormatting.stripFormatting(component.getString());
        if (hover != null && own != null && part.matcher(own.trim()).matches()) {
            return hover;
        }
        for (final Component sibling : component.getSiblings()) {
            final Component found = findHover(sibling, hover, part);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
