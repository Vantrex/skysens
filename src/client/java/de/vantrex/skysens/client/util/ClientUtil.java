package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.config.SkysensConfig;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class ClientUtil {

    private final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public void sendMessage(@NotNull final String message) {
        if (CLIENT.player != null) {
            CLIENT.player.sendMessage(Text.literal(message), false);
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

}
