package de.vantrex.skysens.client.chatcommand;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Holds the chat commands, looked up case-insensitively by name or alias. */
public final class ChatCommandRegistry {

    private final List<ChatCommand> commands = new ArrayList<>();
    private final Map<String, ChatCommand> byLabel = new HashMap<>();

    public void register(@NotNull ChatCommand command) {
        this.commands.add(command);
        this.putLabel(command.name(), command);
        command.aliases().forEach(alias -> this.putLabel(alias, command));
    }

    private void putLabel(String label, ChatCommand command) {
        final ChatCommand previous = this.byLabel.putIfAbsent(label.toLowerCase(Locale.ROOT), command);
        if (previous != null) {
            throw new IllegalStateException("Chat command label '" + label + "' is already used by " + previous.name());
        }
    }

    public @NotNull Optional<ChatCommand> find(@NotNull String label) {
        return Optional.ofNullable(this.byLabel.get(label.toLowerCase(Locale.ROOT)));
    }

    public @NotNull List<ChatCommand> getCommands() {
        return Collections.unmodifiableList(this.commands);
    }

}
