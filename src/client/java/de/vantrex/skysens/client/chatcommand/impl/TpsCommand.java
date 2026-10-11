package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.stats.ServerStatsService;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.OptionalDouble;

/** {@code !tps} sends the server's ticks per second. */
public class TpsCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "tps";
    }

    @Override
    public @NotNull String description() {
        return "Shows the server's ticks per second";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.tps;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final OptionalDouble tps = ServerStatsService.getInstance().getTps();
        context.reply(tps.isPresent()
                ? String.format(Locale.ROOT, "TPS: %.1f", tps.getAsDouble())
                : "TPS: still calculating, try again in a few seconds");
    }

}
