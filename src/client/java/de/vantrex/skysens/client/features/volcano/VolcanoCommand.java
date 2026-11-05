package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/** {@code !volcano} sends the Blazing Volcano state on our Crimson Isle server, e.g. "Volcano: CATACLYSMIC on mini123A". */
public class VolcanoCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "volcano";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("eruption");
    }

    @Override
    public @NotNull String description() {
        return "Shows the volcano state on my Crimson Isle server";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.volcano;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        if (LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.CRIMSON_ISLE) {
            context.reply("Volcano: not on the Crimson Isle");
            return;
        }
        final String state = VolcanoTabList.read();
        final String server = SkysensClient.getInstance().getServerName();
        context.reply("Volcano: " + (state == null ? "unknown" : state.toUpperCase(Locale.ROOT))
                + " on " + (server == null ? "unknown server" : server));
    }

}
