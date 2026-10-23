package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.Zone;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** {@code !zone} sends the island and the zone on it, e.g. "Dwarven Mines - Royal Mines". */
public class ZoneCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "zone";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("where", "area");
    }

    @Override
    public @NotNull String description() {
        return "Shows my island and zone";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.zone;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final LocationService locationService = LocationService.getInstance();
        final SkyblockLocationEnum location = locationService.getCurrentLocation();
        if (location == null) {
            context.reply("Zone: unknown");
            return;
        }
        final Zone<?> zone = locationService.getCurrentZone();
        context.reply(zone == null
                ? "Zone: " + location.getDisplayName()
                : "Zone: " + location.getDisplayName() + " - " + zone.getZoneEnum().getDisplayName());
    }

}
