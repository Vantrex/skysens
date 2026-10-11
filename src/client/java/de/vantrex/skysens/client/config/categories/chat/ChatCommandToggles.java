package de.vantrex.skysens.client.config.categories.chat;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ChatCommandToggles {

    @ConfigOption(name = "!help", desc = "Lists the available commands, or explains one with !help <command>")
    @ConfigEditorBoolean
    @Expose
    public boolean help = true;

    @ConfigOption(name = "!ping", desc = "Sends your current ping")
    @ConfigEditorBoolean
    @Expose
    public boolean ping = true;

    @ConfigOption(name = "!tps", desc = "Sends the server's current TPS")
    @ConfigEditorBoolean
    @Expose
    public boolean tps = true;

}
