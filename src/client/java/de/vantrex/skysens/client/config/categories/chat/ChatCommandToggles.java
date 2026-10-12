package de.vantrex.skysens.client.config.categories.chat;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** Info commands are on by default; commands that act (!warpto), share your position or are just for fun are opt-in. */
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

    @ConfigOption(name = "!profit", desc = "Sends your session profit and profit per hour (current activity and overall); !profit alltime for all time, !profit day for today")
    @ConfigEditorBoolean
    @Expose
    public boolean profit = false;

    @ConfigOption(name = "!zone", desc = "Sends your island and zone")
    @ConfigEditorBoolean
    @Expose
    public boolean zone = true;

    @ConfigOption(name = "!server", desc = "Sends the Hypixel server you are on")
    @ConfigEditorBoolean
    @Expose
    public boolean server = true;

    @ConfigOption(name = "!buffs", desc = "Sends the remaining time of tracked consumable buffs (needs their mining notifications enabled)")
    @ConfigEditorBoolean
    @Expose
    public boolean buffs = false;

    @ConfigOption(name = "!warpto", desc = "When a co-op member types !warpto <your name> and you are not in a party, invite them, warp them to you and disband the party")
    @ConfigEditorBoolean
    @Expose
    public boolean warpTo = false;

    @ConfigOption(name = "!coords", desc = "Sends your coordinates")
    @ConfigEditorBoolean
    @Expose
    public boolean coords = false;

    @ConfigOption(name = "!fps", desc = "Sends your FPS")
    @ConfigEditorBoolean
    @Expose
    public boolean fps = false;

    @ConfigOption(name = "!cf", desc = "Flips a coin")
    @ConfigEditorBoolean
    @Expose
    public boolean coinFlip = false;

    @ConfigOption(name = "!dice", desc = "Rolls a die, optionally with a number of sides: !dice 20")
    @ConfigEditorBoolean
    @Expose
    public boolean dice = false;

    @ConfigOption(name = "!slayer", desc = "Sends this session's slayer stats: bosses killed, bosses per hour, average kill time and profit (see the Slayer category)")
    @ConfigEditorBoolean
    @Expose
    public boolean slayer = false;

}
