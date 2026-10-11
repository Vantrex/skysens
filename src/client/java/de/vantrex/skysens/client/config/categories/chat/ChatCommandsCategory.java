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

    @ConfigOption(name = "Party Commands", desc = "Answer !commands sent in party chat")
    @ConfigEditorBoolean
    @Expose
    public boolean partyEnabled = true;

    @ConfigOption(name = "Only my messages", desc = "Only answer commands you sent yourself, not those of your co-op members")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyOwnMessages = false;

    @ConfigOption(name = "Let other mods answer", desc = "Stay quiet for commands that Odin or SkyHanni already answer, to avoid double replies")
    @ConfigEditorBoolean
    @Expose
    public boolean deferToOtherMods = true;

    @ConfigOption(name = "Auto-accept warp invite", desc = "After you type !warpto <player> in co-op chat, accept that player's party invite automatically for 60 seconds")
    @ConfigEditorBoolean
    @Expose
    public boolean autoAcceptWarpInvite = false;

    @ConfigOption(name = "Cooldown", desc = "Seconds between two answered commands, to avoid being muted for spam")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
    @Expose
    public float cooldownSeconds = 2f;

    @ConfigOption(name = "Commands", desc = "Enable or disable single commands")
    @Accordion
    @Expose
    public ChatCommandToggles commands = new ChatCommandToggles();

    @ConfigOption(name = "Loot Share", desc = "Share your rare drops in co-op chat")
    @Accordion
    @Expose
    public LootShareAccordion lootShare = new LootShareAccordion();

}
