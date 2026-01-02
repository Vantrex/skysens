package de.vantrex.skysens.client;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.service.SensitivityService;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.hypixel.data.type.GameType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.MinecraftClient;

import java.io.File;

@Getter
public class SkysensClient implements ClientModInitializer {

    @Getter
    private static SkysensClient instance;

    private boolean isOnSkyBlock = false;
    private LocationService locationService;

    public SkysensClient() {
        instance = this;

    }

    @Override
    public void onInitializeClient() {
        this.createConfigDirectory();
        SkysensConfig.getConfig().reloadFromFile();
        this.locationService = LocationService.getInstance();
        HypixelModAPI api = HypixelModAPI.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        api.subscribeToEventPacket(ClientboundLocationPacket.class);
        api.createHandler(ClientboundLocationPacket.class, packet -> {
            var serverType = (GameType) packet.getServerType()
                    .filter(serverType1 -> serverType1 instanceof GameType)
                    .orElse(null);
            if (!GameType.SKYBLOCK.equals(serverType)) {
                this.isOnSkyBlock = false;
                return;
            }
            this.isOnSkyBlock = true;
            var serverName = packet.getServerName();
            var lobbyName = packet.getLobbyName().orElse(null);
            var mode = packet.getMode().orElse(null);
            var map = packet.getMap().orElse(null);
            ClientUtil.sendDebug("Server: " + serverName + " | Lobby: " + lobbyName + " | Map: " + map + " | Mode: " + mode);
            this.locationService.handleLocationUpdate(map);

        });

        Runtime.getRuntime().addShutdownHook(new Thread(SkysensConfig.getConfig()::saveToFile));
        registerCommands();
        SensitivityService.getInstance().initConfigHooks();
    }

    private void createConfigDirectory() {
        File globalConfigDir = FabricLoader.getInstance().getConfigDir().toFile();
        if (!globalConfigDir.exists())
            globalConfigDir.mkdirs();
        final File skysensConfigDir = new File(globalConfigDir, "skysens");
        if (!skysensConfigDir.exists())
            skysensConfigDir.mkdirs();
    }


    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("skysens")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        // Schedule the screen opening on the main thread
                        client.send(SkysensConfig.getConfig()::openConfigGui);
                        return 1;
                    }));
        });
    }

}
