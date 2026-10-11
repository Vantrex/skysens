package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** {@code !coords} sends the block position of the player. */
public class CoordsCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "coords";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("co", "pos");
    }

    @Override
    public @NotNull String description() {
        return "Shows my coordinates";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.coords;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        final BlockPos pos = player.blockPosition();
        context.reply("x: " + pos.getX() + ", y: " + pos.getY() + ", z: " + pos.getZ());
    }

}
