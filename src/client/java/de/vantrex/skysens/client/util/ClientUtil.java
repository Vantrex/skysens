package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
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

}
