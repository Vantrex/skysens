package de.vantrex.skysens.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.VisualsSettingsTransfer;
import de.vantrex.skysens.client.config.ui.SkysensConfigScreen;
import de.vantrex.skysens.client.handler.ChatHandler;
import de.vantrex.skysens.client.handler.KeyHandlers;
import de.vantrex.skysens.client.handler.TickHandlers;
import de.vantrex.skysens.client.handler.UseHandlers;
import de.vantrex.skysens.client.feature.dungeon.RewardChestFeature;
import de.vantrex.skysens.client.profit.ProfitService;
import de.vantrex.skysens.client.service.FeatureService;
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
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public class SkysensClient implements ClientModInitializer {

    @Getter
    private static SkysensClient instance;
    // Daemon threads so the scheduler never keeps the JVM alive after Minecraft stops
    // (otherwise the client shutdown watchdog writes a crash report).
    public static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "SkySens Scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private boolean isOnSkyBlock = false;
    /** Name of the Hypixel server we are on, e.g. "mini123AB", from the Mod API location packet. */
    private String serverName;
    private LocationService locationService;

    public SkysensClient() {
        instance = this;
    }

    @Override
    public void onInitializeClient() {
        this.createConfigDirectory();
        RepositoryRegistry.loadRepositories();
        SkysensConfig.getConfig(); // creating the config loads it from disk
        this.locationService = LocationService.getInstance();
        this.initHypixelApiHandler();
        Runtime.getRuntime().addShutdownHook(new Thread(SkysensConfig.getConfig()::saveToFile));
        Runtime.getRuntime().addShutdownHook(new Thread(RepositoryRegistry::saveRepositories));
        registerCommands();
        SensitivityService.getInstance().initConfigHooks();
        UseHandlers.register();
        TickHandlers.register();
        ChatHandler.register();
        KeyHandlers.register();
        ProfitService.getInstance(); // starts counting time on SkyBlock
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
            this.serverName = packet.getServerName();
            var serverType = (GameType) packet.getServerType()
                    .filter(serverType1 -> serverType1 instanceof GameType)
                    .orElse(null);
            if (!GameType.SKYBLOCK.equals(serverType)) {
                this.isOnSkyBlock = false;
                return;
            }
            this.isOnSkyBlock = true;
            var lobbyName = packet.getLobbyName().orElse(null);
            var mode = packet.getMode().orElse(null);
            var map = packet.getMap().orElse(null);
            ClientUtil.sendDebug("Server: " + this.serverName + " | Lobby: " + lobbyName + " | Map: " + map + " | Mode: " + mode);
            this.locationService.handleLocationUpdate(map);
        });
    }

    private int showChestProfit() {
        ClientUtil.sendMessage(RewardChestFeature.summary("session", FeatureService.getInstance().getRewardChestFeature().getSession()));
        ClientUtil.sendMessage(RewardChestFeature.summary("all time", RepositoryRegistry.PROFIT_REPOSITORY.dungeonChests));
        return 1;
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("skysens")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        // Schedule the screen opening on the main thread
                        client.execute(() -> {
                            // Don't return to the chat screen the command was typed in
                            Screen parent = client.gui.screen() instanceof ChatScreen ? null : client.gui.screen();
                            client.gui.setScreen(new SkysensConfigScreen(parent));
                        });
                        return 1;
                    })
                    .then(ClientCommands.literal("scale")
                            .then(ClientCommands.literal("export")
                                    .executes(context -> VisualsSettingsTransfer.export()))
                            .then(ClientCommands.literal("import")
                                    .executes(context -> VisualsSettingsTransfer.importFrom(Minecraft.getInstance().keyboardHandler.getClipboard()))
                                    .then(ClientCommands.argument("data", StringArgumentType.greedyString())
                                            .executes(context -> VisualsSettingsTransfer.importFrom(StringArgumentType.getString(context, "data")))))
                            .then(ClientCommands.literal("reset")
                                    .executes(context -> VisualsSettingsTransfer.reset())))
                    .then(ClientCommands.literal("dungeon")
                            .then(ClientCommands.literal("profit")
                                    .executes(context -> this.showChestProfit())
                                    .then(ClientCommands.literal("reset")
                                            .executes(context -> {
                                                FeatureService.getInstance().getRewardChestFeature().getSession().reset();
                                                ProfitService.getInstance().resetSessionChests();
                                                ClientUtil.sendMessage("§5[SkySens] §7Session chest profit reset.");
                                                return 1;
                                            })
                                            .then(ClientCommands.literal("alltime")
                                                    .executes(context -> {
                                                        RepositoryRegistry.PROFIT_REPOSITORY.dungeonChests.reset();
                                                        ProfitService.getInstance().resetAllTimeChests();
                                                        ClientUtil.sendMessage("§5[SkySens] §7All-time chest profit reset.");
                                                        return 1;
                                                    }))))));
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
