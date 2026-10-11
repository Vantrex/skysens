package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

/** {@code !fps} sends the current frames per second. */
public class FpsCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "fps";
    }

    @Override
    public @NotNull String description() {
        return "Shows my FPS";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.fps;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        context.reply("FPS: " + Minecraft.getInstance().getFps());
    }

}
