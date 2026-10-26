package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.notification.SecondaryActionBar;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@UtilityClass
public class ClientUtil {

    private final Minecraft CLIENT = Minecraft.getInstance();
    /**
     * -- GETTER --
     * True while SkySens itself is showing a message.
     */
    @Getter
    private boolean sendingOwnMessage;

    public void sendMessage(@NotNull final String message) {
        sendMessage(Component.literal(message));
    }

    /** Sends a message behind the "[SkySens]" gradient prefix. */
    public void sendPrefixed(@NotNull final String message) {
        sendMessage(Brand.prefix().append(Component.literal(message)));
    }

    public void sendMessage(final @NotNull Component text) {
        sendMessage(text, false);
    }

    public void sendMessage(final @NotNull Component text, final boolean overlay) {
        if (CLIENT.player == null)
            return;
        // these fire the game message event, our listeners must not handle our own messages
        sendingOwnMessage = true;
        try {
            if (overlay) {
                CLIENT.player.sendOverlayMessage(text);
            } else {
                CLIENT.player.sendSystemMessage(text);
            }
        } finally {
            sendingOwnMessage = false;
        }
    }

    public void sendDebug(final @NotNull String message) {
        if (SkysensConfig.getInstance().devCategory.debugMode) {
            sendMessage("§8[§cDEBUG§8] §f" + message);
        }
    }

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (CLIENT.player == null) return;
        CLIENT.player.playSound(soundEvent, volume, pitch);
    }

    public void sendTitle(final @NotNull String title) {
        sendTitle(title, "");
    }

    public void sendSubtitle(final @NotNull String subtitle) {
        sendTitle(null, subtitle);
    }

    public void sendSubtitle(final @NotNull Component subtitle) {
        sendTitle(null, subtitle, 0, 60, 0);
    }

    public void sendTitle(final @NotNull Component title) {
        sendTitle(title, null, 0, 60, 0);
    }

    public void sendTitle(final @Nullable String title, final @Nullable String subtitle) {
        sendTitle(
                Optional.ofNullable(title).map(Component::literal).orElse(null),
                Optional.ofNullable(subtitle).map(Component::literal).orElse(null),
                0, 60, 0
        );
    }

    public void sendTitle(final @Nullable Component title, final @Nullable Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (CLIENT.player == null) return;
        if (CLIENT.gui == null) return;
        if (title != null)
            CLIENT.gui.hud.setTitle(title);
        if (subtitle != null)
            CLIENT.gui.hud.setSubtitle(subtitle);
        CLIENT.gui.hud.setTimes(fadeInTicks, stayTicks, fadeOutTicks);
    }


    public static void sendActionBar(@NotNull Component text) {
        if (CLIENT.player == null) return;
        if (CLIENT.gui == null) return;
        CLIENT.gui.hud.setOverlayMessage(text, false);
    }

    /** Shows the text in the line above the action bar, unless another message is still shown there. */
    public void sendSecondaryActionBar(@NotNull Component text, long durationMs) {
        sendSecondaryActionBar(text, durationMs, false);
    }

    /**
     * Shows the text in the line above the action bar for the given time. It is drawn on its own line, so Hypixel's
     * action bar (health, defense, mana) doesn't overwrite it. With override it replaces a message that is still shown.
     */
    public void sendSecondaryActionBar(@NotNull Component text, long durationMs, boolean override) {
        SecondaryActionBar.getInstance().show(text, durationMs, override);
    }
}
