package de.vantrex.skysens.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.handler.ChatHandler;
import de.vantrex.skysens.client.handler.TickHandlers;
import de.vantrex.skysens.client.handler.UseHandlers;
import de.vantrex.skysens.client.dungeon.DungeonStateManager;
import de.vantrex.skysens.client.repository.RepositoryRegistry;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.service.SensitivityService;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.hypixel.data.type.GameType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public class SkysensClient implements ClientModInitializer {

    @Getter
    private static SkysensClient instance;
    public static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    private boolean isOnSkyBlock = false;
    private LocationService locationService;

    public SkysensClient() {
        instance = this;
    }

    @Override
    public void onInitializeClient() {
        this.createConfigDirectory();
        RepositoryRegistry.loadRepositories();
        SkysensConfig.getConfig().reloadFromFile();
        this.locationService = LocationService.getInstance();
        this.initHypixelApiHandler();
        Runtime.getRuntime().addShutdownHook(new Thread(SkysensConfig.getConfig()::saveToFile));
        Runtime.getRuntime().addShutdownHook(new Thread(RepositoryRegistry::saveRepositories));
        registerCommands();
        SensitivityService.getInstance().initConfigHooks();
        UseHandlers.register();
        TickHandlers.register();
        ChatHandler.register();
        DungeonStateManager.getInstance();
    }

    private void createConfigDirectory() {
        File globalConfigDir = FabricLoader.getInstance().getConfigDir().toFile();
        if (!globalConfigDir.exists())
            globalConfigDir.mkdirs();
        final File skysensConfigDir = new File(globalConfigDir, "skysens");
        if (!skysensConfigDir.exists())
            skysensConfigDir.mkdirs();
    }

    private void initHypixelApiHandler() {
        final var api = HypixelModAPI.getInstance();
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
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("skysens")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        // Schedule the screen opening on the main thread
                        client.execute(SkysensConfig.getConfig()::openConfigGui);
                        return 1;
                    }));
            dispatcher.register(ClientCommands.literal("mocklocation")
                    .then(ClientCommands.argument("location", StringArgumentType.string())
                            .executes(context -> {
                                String location = StringArgumentType.getString(context, "location");
                                if (location.equals("null")) {
                                    location = null;
                                }
                                this.locationService.handleLocationUpdate(location);
                                return 1;
                            })));
        });
    }

}
