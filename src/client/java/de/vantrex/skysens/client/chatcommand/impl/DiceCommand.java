package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** {@code !dice [sides]} rolls a die with 6 or the given number of sides. */
public class DiceCommand implements ChatCommand {

    private static final int DEFAULT_SIDES = 6;
    private static final int MAX_SIDES = 1000;

    @Override
    public @NotNull String name() {
        return "dice";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("roll");
    }

    @Override
    public @NotNull String description() {
        return "Rolls a die, 6 sides by default";
    }

    @Override
    public @NotNull String usage() {
        return "[sides]";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.dice;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        int sides = DEFAULT_SIDES;
        if (context.hasArgs()) {
            try {
                sides = Integer.parseInt(context.arg(0));
            } catch (NumberFormatException e) {
                context.reply("Usage: dice [sides], e.g. dice 20");
                return;
            }
            if (sides < 2 || sides > MAX_SIDES) {
                context.reply("A die needs 2 to " + MAX_SIDES + " sides");
                return;
            }
        }
        final int roll = ThreadLocalRandom.current().nextInt(1, sides + 1);
        context.reply(context.sender() + " rolled a " + roll + " (d" + sides + ")");
    }

}
