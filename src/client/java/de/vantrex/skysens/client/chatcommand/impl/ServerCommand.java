package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** {@code !server} sends the name of the Hypixel server we are on. */
public class ServerCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "server";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("lobby");
    }

    @Override
    public @NotNull String description() {
        return "Shows the server I'm on";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.server;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final String serverName = SkysensClient.getInstance().getServerName();
        context.reply("Server: " + (serverName == null ? "unknown" : serverName));
    }

}
