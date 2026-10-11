package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.chatcommand.ChatCommandRegistry;
import de.vantrex.skysens.client.chatcommand.ChatCommandService;
import de.vantrex.skysens.client.config.SkysensConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/** {@code !help} lists all enabled commands, {@code !help <command>} explains one. */
public class HelpCommand implements ChatCommand {

    private final ChatCommandRegistry registry;

    public HelpCommand(@NotNull ChatCommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public @NotNull String name() {
        return "help";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("h", "commands");
    }

    @Override
    public @NotNull String description() {
        return "Lists the commands or explains one";
    }

    @Override
    public @NotNull String usage() {
        return "[command]";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.help;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        if (context.hasArgs()) {
            String label = context.arg(0);
            if (label.startsWith(ChatCommandService.PREFIX)) {
                label = label.substring(ChatCommandService.PREFIX.length());
            }
            final String finalLabel = label;
            this.registry.find(label)
                    .filter(ChatCommand::isEnabled)
                    .ifPresentOrElse(
                            command -> context.reply(describe(command)),
                            () -> context.reply("Unknown command: " + finalLabel));
            return;
        }
        final String commands = this.registry.getCommands().stream()
                .filter(ChatCommand::isEnabled)
                .map(command -> ChatCommandService.PREFIX + command.name())
                .collect(Collectors.joining(", "));
        context.reply("Commands: " + commands + " (" + ChatCommandService.PREFIX + "help <command> for details)");
    }

    private static String describe(ChatCommand command) {
        final String usage = command.usage().isEmpty() ? "" : " " + command.usage();
        return "Usage: " + ChatCommandService.PREFIX + command.name() + usage + " - " + command.description();
    }

}
