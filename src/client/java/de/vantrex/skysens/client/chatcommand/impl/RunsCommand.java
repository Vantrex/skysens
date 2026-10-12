package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.dungeon.RunStatsAccordion;
import de.vantrex.skysens.client.feature.dungeon.DungeonRunStats;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * {@code !runs [floor]} sends the dungeon run stats of this session for the most recent floor (or the given one),
 * e.g. "M7: 5 runs | avg 6m02s (best 5m41s) | S+ 80% | 0.4 deaths/run".
 * The fields are configurable. When several floors were run, the total run count is appended.
 */
public class RunsCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "runs";
    }

    @Override
    public @NotNull String description() {
        return "Shows my dungeon run stats of this session";
    }

    @Override
    public @NotNull String usage() {
        return "[floor]";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.runs
                && SkysensConfig.getInstance().dungeonCategory.runStats.enabled;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final DungeonRunStats stats = DungeonRunStats.getInstance();
        final String floor = context.hasArgs() ? stats.find(context.arg(0)) : stats.getLatestFloor();
        final DungeonRunStats.FloorStats floorStats = floor == null ? null : stats.get(floor);
        if (floorStats == null) {
            context.reply(context.hasArgs()
                    ? "No " + context.arg(0).toUpperCase(Locale.ROOT) + " runs this session"
                    : "No dungeon runs this session");
            return;
        }
        final RunStatsAccordion config = SkysensConfig.getInstance().dungeonCategory.runStats;
        final StringBuilder reply = new StringBuilder(floor).append(": ").append(floorStats.runs)
                .append(floorStats.runs == 1 ? " run" : " runs");
        if (config.replyTimes) {
            reply.append(" | avg ").append(DungeonRunStats.formatTime(floorStats.avgSeconds()))
                    .append(" (best ").append(DungeonRunStats.formatTime(floorStats.bestSeconds)).append(")");
        }
        if (config.replyScore) {
            reply.append(" | S+ ").append(Math.round(floorStats.sPlusRate() * 100)).append("%");
        }
        if (config.replyDeaths) {
            reply.append(String.format(Locale.ROOT, " | %.1f deaths/run", floorStats.deathsPerRun()));
        }
        if (stats.getTotalRuns() > floorStats.runs) {
            reply.append(" | ").append(stats.getTotalRuns()).append(" runs total");
        }
        context.reply(reply.toString());
    }

}
