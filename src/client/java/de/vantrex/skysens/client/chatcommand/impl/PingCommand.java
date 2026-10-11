package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.stats.ServerStatsService;
import org.jetbrains.annotations.NotNull;

/** {@code !ping} sends the current ping. */
public class PingCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "ping";
    }

    @Override
    public @NotNull String description() {
        return "Shows my ping";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.ping;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        ServerStatsService.getInstance().getPing().thenAccept(ping -> context.reply(ping.isPresent()
                ? "Ping: " + ping.getAsInt() + "ms"
                : "Ping: unknown (no response from server)"));
    }

}
