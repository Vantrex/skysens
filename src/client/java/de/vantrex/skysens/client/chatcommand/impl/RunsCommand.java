package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.dungeon.RunStatsAccordion;
import de.vantrex.skysens.client.feature.dungeon.DungeonRunStats;
import de.vantrex.skysens.client.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * {@code !runs [floor] [day|alltime]} sends the dungeon run stats of this session (or today, or all time) for the most
 * recent floor (or the given one), e.g.
 * "M7: 5 runs | avg 6m02s (best 5m41s) | S+ 80% | 0.4 deaths/run | chests +38.2M (7.6M/run, 76.0M/h)".
 * The fields are configurable. When several floors were run, the total run count and chest profit are appended.
 */
public class RunsCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "runs";
    }

    @Override
    public @NotNull String description() {
        return "Shows my dungeon run stats of this session, today or all time";
    }

    @Override
    public @NotNull String usage() {
        return "[floor] [day|alltime]";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.runs
                && SkysensConfig.getInstance().dungeonCategory.runStats.enabled;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        DungeonRunStats.Scope scope = DungeonRunStats.Scope.SESSION;
        String query = null;
        for (final String arg : context.args()) {
            final DungeonRunStats.Scope parsed = DungeonRunStats.Scope.parse(arg);
            if (parsed != null) {
                scope = parsed;
            } else {
                query = arg;
            }
        }
        final DungeonRunStats stats = DungeonRunStats.getInstance();
        final String floor = query != null ? stats.find(scope, query) : stats.getLatestFloor(scope);
        final DungeonRunStats.FloorStats floorStats = floor == null ? null : stats.get(scope, floor);
        if (floorStats == null) {
            context.reply((query != null ? "No " + query.toUpperCase(Locale.ROOT) + " runs " : "No dungeon runs ")
                    + scope.noRunsSuffix);
            return;
        }
        final RunStatsAccordion config = SkysensConfig.getInstance().dungeonCategory.runStats;
        final StringBuilder reply = new StringBuilder(switch (scope) {
            case SESSION -> "";
            case DAY -> "Today ";
            case ALL_TIME -> "All-time ";
        }).append(floor).append(": ").append(floorStats.runs).append(floorStats.runs == 1 ? " run" : " runs");
        if (floorStats.runs > 0) {
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
        }
        if (config.replyProfit && floorStats.chests > 0) {
            reply.append(" | chests ").append(signed(floorStats.chestProfit));
            if (floorStats.runs > 0) {
                reply.append(" (").append(signed(floorStats.chestProfitPerRun())).append("/run, ")
                        .append(signed(floorStats.chestProfitPerHour())).append("/h)");
            }
        }
        final int totalRuns = stats.getTotalRuns(scope);
        if (totalRuns > floorStats.runs) {
            reply.append(" | ").append(totalRuns).append(" runs total");
            final double totalProfit = stats.getTotalChestProfit(scope);
            if (config.replyProfit && totalProfit != floorStats.chestProfit) {
                reply.append(", chests ").append(signed(totalProfit));
            }
        }
        context.reply(reply.toString());
    }

    private static String signed(double coins) {
        return (coins < 0 ? "-" : "+") + NumberUtil.formatCoins(Math.abs(coins));
    }

}
