package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.stats.ClientStats;
import de.vantrex.skysens.client.stats.InternalStatsProvider;
import de.vantrex.skysens.client.util.TimeUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** {@code !uptime} sends the uptime of the player on skyblock. */
public class UptimeCommand implements ChatCommand {

    /** {@code zzzz} resolves DST, e.g. "Central European Summer Time" instead of "Central European Time" */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm (zzzz)", Locale.ENGLISH);
    private static final String RESPONSE = "Current uptime: %s | Accumulated session uptime (with rejoins): %s";
    private static final String LAST_JOINED = " | Last joined Skyblock at: %s";

    @Override
    public @NotNull String name() {
        return "uptime";
    }

    @Override
    public @NotNull String description() {
        return "Shows the uptime of a player";
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        if (!SkysensClient.getInstance().isOnSkyBlock()) {
            context.reply("Uptime: Not on Skyblock!");
            return;
        }
        context.reply(this.buildClientStats());
    }

    private String buildClientStats() {
        final ClientStats clientStats = InternalStatsProvider.getInstance().getClientStats();
        return RESPONSE.formatted(
                TimeUtil.formatDuration(clientStats.getCurrentSessionPlaytime()),
                TimeUtil.formatDuration(clientStats.getAccumulatedSessionPlaytime())
        ) + buildLastJoined(clientStats);
    }

    private String buildLastJoined(final ClientStats clientStats) {
        if (clientStats.getCurrentSessionJoin() == null) {
            return "<Error> Please report this!";
        }
        final var lastJoinedAt = Instant.ofEpochMilli(clientStats.getCurrentSessionJoin()).atZone(ZoneId.systemDefault());
        return LAST_JOINED.formatted(DATE_TIME_FORMATTER.format(lastJoinedAt));
    }

}
