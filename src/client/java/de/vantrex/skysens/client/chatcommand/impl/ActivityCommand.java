package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.activity.ActivityTracker;
import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.chat.ActivityAccordion;
import de.vantrex.skysens.client.util.TimeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code !activity [n]} sends the latest activities, most recent first, with counts, durations and how long ago,
 * e.g. "Recent: M7 x5 (32m, 3m ago) | Inferno Demonlord IV x14 (25m, 40m ago) | now: Dungeon Hub".
 * Entries that don't fit into a chat message are dropped.
 */
public class ActivityCommand implements ChatCommand {

    /** Room left for the channel command ("/cchat ") in front of the reply. */
    private static final int MAX_LENGTH = 245;
    private static final int MAX_COUNT = 10;

    @Override
    public @NotNull String name() {
        return "activity";
    }

    @Override
    public @NotNull String description() {
        return "Shows my latest activities";
    }

    @Override
    public @NotNull String usage() {
        return "[count]";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.activity;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final ActivityAccordion config = SkysensConfig.getInstance().chatCommandsCategory.activity;
        int count = Math.round(config.defaultCount);
        if (context.hasArgs()) {
            try {
                count = Integer.parseInt(context.arg(0));
            } catch (NumberFormatException ignored) {
                // keep the default
            }
        }
        count = Math.max(1, Math.min(MAX_COUNT, count));

        final ActivityTracker tracker = ActivityTracker.getInstance();
        final long now = System.currentTimeMillis();
        final List<String> parts = new ArrayList<>();
        for (final ActivityTracker.Entry entry : tracker.recent(count)) {
            parts.add(format(entry, now));
        }
        final String current = config.includeCurrent ? tracker.currentLocation() : null;
        final String nowPart = current == null ? "" : "now: " + current;

        final StringBuilder reply = new StringBuilder();
        if (!parts.isEmpty()) {
            reply.append("Recent: ");
            int shown = 0;
            for (final String part : parts) {
                final int needed = reply.length() + (shown > 0 ? 3 : 0) + part.length() + (nowPart.isEmpty() ? 0 : 3 + nowPart.length());
                if (needed > MAX_LENGTH && shown > 0) {
                    break;
                }
                if (shown > 0) {
                    reply.append(" | ");
                }
                reply.append(part);
                shown++;
            }
        }
        if (!nowPart.isEmpty()) {
            reply.append(reply.isEmpty() ? "" : " | ").append(nowPart);
        }
        if (reply.isEmpty()) {
            reply.append("No recent activity");
        }
        context.reply(reply.length() > MAX_LENGTH ? reply.substring(0, MAX_LENGTH) : reply.toString());
    }

    private static String format(ActivityTracker.Entry entry, long now) {
        final StringBuilder text = new StringBuilder(entry.label);
        if (entry.type.isCountable() && entry.count > 1) {
            text.append(" x").append(entry.count);
        }
        text.append(" (");
        if (entry.duration() > 0) {
            text.append(TimeUtil.formatHoursMinutes(entry.duration())).append(", ");
        }
        text.append(TimeUtil.formatHoursMinutes(now - entry.end)).append(" ago)");
        return text.toString();
    }

}
