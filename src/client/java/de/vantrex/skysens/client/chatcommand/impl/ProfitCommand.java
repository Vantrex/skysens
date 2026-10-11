package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.profit.ProfitService;
import de.vantrex.skysens.client.profit.ProfitStats;
import de.vantrex.skysens.client.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

/**
 * {@code !profit [alltime]} sends the profit of the current activity (from SkyHanni's tracker when installed)
 * and the overall profit, each with profit per hour, e.g.
 * "Slayer: 45M (12.3M/h) | Overall: 80M (20M/h)".
 */
public class ProfitCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "profit";
    }

    @Override
    public @NotNull String description() {
        return "Shows my session profit and profit per hour";
    }

    @Override
    public @NotNull String usage() {
        return "[alltime]";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.profit
                && SkysensConfig.getInstance().generalCategory.profitTracker.enabled;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final boolean allTime = context.hasArgs() && context.arg(0).equalsIgnoreCase("alltime");
        final ProfitService service = ProfitService.getInstance();
        final StringBuilder reply = new StringBuilder(allTime ? "All-time " : "Session ");
        final ProfitService.ActivityProfit activity = service.getCurrentActivity(allTime);
        if (activity != null) {
            reply.append(activity.name()).append(": ").append(format(activity.stats())).append(" | ");
        }
        reply.append("Overall: ").append(format(service.getOverall(allTime)));
        context.reply(reply.toString());
    }

    private static String format(ProfitStats stats) {
        final String profit = (stats.profit < 0 ? "-" : "") + NumberUtil.formatCoins(Math.abs(stats.profit));
        final double perHour = stats.perHour();
        if (Double.isNaN(perHour)) {
            return profit;
        }
        return profit + " (" + (perHour < 0 ? "-" : "") + NumberUtil.formatCoins(Math.abs(perHour)) + "/h)";
    }

}
