package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.config.SkysensConfig;
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

    public void sendMessage(@NotNull final String message) {
        sendMessage(Component.literal(message));
    }

    public void sendMessage(final @NotNull Component text) {
        sendMessage(text, false);
    }

    public void sendMessage(final @NotNull Component text, final boolean overlay) {
        if (CLIENT.player == null)
            return;
        if (overlay) {
            CLIENT.player.sendOverlayMessage(text);
        } else {
            CLIENT.player.sendSystemMessage(text);
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
            CLIENT.gui.setTitle(title);
        if (subtitle != null)
            CLIENT.gui.setSubtitle(subtitle);
        CLIENT.gui.setTimes(fadeInTicks, stayTicks, fadeOutTicks);
    }


    public static void sendActionBar(@NotNull Component text) {
        if (CLIENT.player == null) return;
        if (CLIENT.gui == null) return;
        CLIENT.gui.setOverlayMessage(text, false);
    }
}
