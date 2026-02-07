package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.config.SkysensConfig;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@UtilityClass
public class ClientUtil {

    private final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public void sendMessage(@NotNull final String message) {
        sendMessage(Text.literal(message));
    }

    public void sendMessage(final @NotNull Text text) {
        sendMessage(text, false);
    }

    public void sendMessage(final @NotNull Text text, final boolean overlay) {
        if (CLIENT.player == null)
            return;
        CLIENT.player.sendMessage(text, overlay);
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

    public void sendSubtitle(final @NotNull Text subtitle) {
        sendTitle(null, subtitle, 0, 60, 0);
    }

    public void sendTitle(final @NotNull Text title) {
        sendTitle(title, null, 0, 60, 0);
    }

    public void sendTitle(final @Nullable String title, final @Nullable String subtitle) {
        sendTitle(
                Optional.ofNullable(title).map(Text::literal).orElse(null),
                Optional.ofNullable(subtitle).map(Text::literal).orElse(null),
                0, 60, 0
        );
    }

    public void sendTitle(final @Nullable Text title, final @Nullable Text subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (CLIENT.player == null) return;
        if (CLIENT.inGameHud == null) return;
        if (title != null)
            CLIENT.inGameHud.setTitle(title);
        if (subtitle != null)
            CLIENT.inGameHud.setSubtitle(subtitle);
        CLIENT.inGameHud.setTitleTicks(fadeInTicks, stayTicks, fadeOutTicks);
    }


    public static void sendActionBar(@NotNull Text text) {
        if (CLIENT.player == null) return;
        if (CLIENT.inGameHud == null) return;
        CLIENT.inGameHud.setOverlayMessage(text, false);
    }

    /**
     * Strips Minecraft legacy formatting codes (§x) from the given string.
     * @param text The text to strip formatting codes from
     * @return The text without formatting codes
     */
    public static String stripFormattingCodes(@NotNull String text) {
        return text.replaceAll("§.", "");
    }
}
