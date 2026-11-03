package de.vantrex.skysens.client.integration.odin;

import de.vantrex.skysens.client.chatcommand.ChatCommandBridge;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.integration.KotlinObjectAccessor;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Odin's "Chat Commands" module answers many of the same !commands in co-op and party chat. This tells
 * whether Odin will answer a label, so we can stay quiet instead of sending a second reply.
 */
public final class OdinChatCommandsBridge implements ChatCommandBridge {

    /** Odin command labels mapped to the name of the setting that toggles them (see Odin's ChatCommands). */
    private static final Map<String, String> SETTING_BY_LABEL = Map.ofEntries(
            Map.entry("coords", "Coords (coords)"),
            Map.entry("co", "Coords (coords)"),
            Map.entry("odin", "Odin"),
            Map.entry("od", "Odin"),
            Map.entry("boop", "Boop"),
            Map.entry("cf", "Coinflip (cf)"),
            Map.entry("8ball", "Eightball"),
            Map.entry("dice", "Dice"),
            Map.entry("racism", "Racism"),
            Map.entry("ping", "Ping"),
            Map.entry("tps", "Tps"),
            Map.entry("fps", "FPS"),
            Map.entry("time", "Time"),
            Map.entry("location", "Location"),
            Map.entry("holding", "Holding")
    );
    private static final Map<ChatChannel, String> CHANNEL_SETTING = Map.of(
            ChatChannel.COOP, "Co-op Commands",
            ChatChannel.PARTY, "Party Commands"
    );

    private final boolean loaded = FabricLoader.getInstance().isModLoaded("odin");
    private final KotlinObjectAccessor chatCommands = new KotlinObjectAccessor("com.odtheking.odin.features.impl.skyblock.ChatCommands");
    private Method settingValueGetter;

    @Override
    public @NotNull String name() {
        return "Odin";
    }

    @Override
    public boolean handles(@NotNull ChatChannel channel, @NotNull String label, @NotNull String sender) {
        if (!this.loaded || !Boolean.TRUE.equals(this.chatCommands.call("getEnabled"))) {
            return false;
        }
        final String channelSetting = CHANNEL_SETTING.get(channel);
        if (channelSetting == null || !this.isSettingOn(channelSetting)) {
            return false;
        }
        // Odin always answers help, even though it has no toggle for it
        if (label.equals("help") || label.equals("h")) {
            return true;
        }
        final String setting = SETTING_BY_LABEL.get(label);
        return setting != null && this.isSettingOn(setting);
    }

    private boolean isSettingOn(String name) {
        if (!(this.chatCommands.call("getSettings") instanceof Map<?, ?> settings)) {
            return false;
        }
        final Object setting = settings.get(name);
        if (setting == null) {
            return false;
        }
        try {
            if (this.settingValueGetter == null) {
                this.settingValueGetter = setting.getClass().getMethod("getValue");
            }
            return Boolean.TRUE.equals(this.settingValueGetter.invoke(setting));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

}
