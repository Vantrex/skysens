package de.vantrex.skysens.client.service;

import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Getter
public class PlayerService {

    @Getter
    private static final PlayerService instance = new PlayerService();

    private final MinecraftClient client = MinecraftClient.getInstance();
    private String currentPlayerName;

    private PlayerService() {
        ClientTickEvents.END_WORLD_TICK.register(this::updatePlayerName);
    }

    private void updatePlayerName(ClientWorld clientWorld) {
        if (client.player != null) {
            this.currentPlayerName = client.player.getName().getString();
        } else {
            this.currentPlayerName = null;
        }
    }

}
