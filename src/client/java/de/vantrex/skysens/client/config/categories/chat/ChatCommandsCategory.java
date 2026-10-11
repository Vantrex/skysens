package de.vantrex.skysens.client.config.categories.chat;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ChatCommandsCategory {

    @ConfigOption(name = "Co-op Commands", desc = "Answer !commands (e.g. !ping, !tps) sent in co-op chat")
    @ConfigEditorBoolean
    @Expose
    public boolean coopEnabled = true;

    @ConfigOption(name = "Only my messages", desc = "Only answer commands you sent yourself, not those of your co-op members")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyOwnMessages = false;

    @ConfigOption(name = "Let Odin answer", desc = "Stay quiet for commands that Odin's Chat Commands module already answers, to avoid double replies")
    @ConfigEditorBoolean
    @Expose
    public boolean deferToOdin = true;

    @ConfigOption(name = "Cooldown", desc = "Seconds between two answered commands, to avoid being muted for spam")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
    @Expose
    public float cooldownSeconds = 2f;

    @ConfigOption(name = "Commands", desc = "Enable or disable single commands")
    @Accordion
    @Expose
    public ChatCommandToggles commands = new ChatCommandToggles();

}
