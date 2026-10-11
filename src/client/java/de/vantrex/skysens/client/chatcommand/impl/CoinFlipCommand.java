package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** {@code !cf} flips a coin. */
public class CoinFlipCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "cf";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("coinflip", "flip");
    }

    @Override
    public @NotNull String description() {
        return "Flips a coin";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.coinFlip;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        context.reply(context.sender() + " flipped " + (ThreadLocalRandom.current().nextBoolean() ? "heads" : "tails"));
    }

}
