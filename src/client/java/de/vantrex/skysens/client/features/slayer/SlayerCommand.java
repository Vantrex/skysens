package de.vantrex.skysens.client.features.slayer;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import org.jetbrains.annotations.NotNull;

/**
 * {@code !slayer} sends this session's slayer stats per boss, e.g.
 * "Inferno Demonlord IV: 42 bosses (31.2/h) | avg kill 38s | 1.4M/boss (43.7M/h)".
 * Which values are included is configured under Slayer.
 */
public class SlayerCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "slayer";
    }

    @Override
    public @NotNull String description() {
        return "Shows my slayer boss kills, kill times and profit this session";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.slayer
                && SkysensConfig.getInstance().slayerCategory.enabled;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        context.reply(SlayerService.getInstance().summary());
    }

}
