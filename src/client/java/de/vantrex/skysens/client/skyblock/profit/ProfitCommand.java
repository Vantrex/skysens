package de.vantrex.skysens.client.skyblock.profit;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

/**
 * {@code !profit [alltime|day]} sends the profit of the current activity (from SkyHanni's tracker when installed)
 * and the overall profit, each with profit per hour, e.g.
 * "Session Slayer (Inferno Demonlord IV): 73.2M (62.1M/h active) | Overall: 73.2M (54.9M/h on SkyBlock)".
 * The rate is labeled with the time it is based on: "active" (time actually farming) or "on SkyBlock".
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
        return "[alltime|day]";
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
        if (context.hasArgs() && (context.arg(0).equalsIgnoreCase("day") || context.arg(0).equalsIgnoreCase("today"))) {
            context.reply("Today: " + format(service.getDay().stats(), "on SkyBlock"));
            return;
        }
        final StringBuilder reply = new StringBuilder(allTime ? "All-time " : "Session ");
        final ProfitService.ActivityProfit activity = service.getCurrentActivity(allTime);
        if (activity != null) {
            reply.append(activity.name()).append(": ").append(format(activity.stats(), "active")).append(" | ");
        }
        final ProfitService.OverallProfit overall = service.getOverall(allTime);
        reply.append("Overall: ").append(format(overall.stats(), overall.timeOnSkyBlock() ? "on SkyBlock" : "active"));
        context.reply(reply.toString());
    }

    private static String format(ProfitStats stats, String rateBasis) {
        final String profit = (stats.profit < 0 ? "-" : "") + NumberUtil.formatCoins(Math.abs(stats.profit));
        final double perHour = stats.perHour();
        if (Double.isNaN(perHour)) {
            return profit;
        }
        return profit + " (" + (perHour < 0 ? "-" : "") + NumberUtil.formatCoins(Math.abs(perHour)) + "/h " + rateBasis + ")";
    }

}
