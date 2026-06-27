package de.vantrex.skysens.client.service;

import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Getter
public class PlayerService {

    @Getter
    private static final PlayerService instance = new PlayerService();

    private final Minecraft client = Minecraft.getInstance();
    private String currentPlayerName;

    private PlayerService() {
        ClientTickEvents.END_LEVEL_TICK.register(this::updatePlayerName);
    }

    private void updatePlayerName(ClientLevel clientWorld) {
        if (client.player != null) {
            this.currentPlayerName = client.player.getName().getString();
        } else {
            this.currentPlayerName = null;
        }
    }

}
