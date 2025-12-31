package de.vantrex.skysens.client;

import de.vantrex.skysens.client.gui.config.ConfigCategory;
import de.vantrex.skysens.client.gui.config.ConfigScreen;
import de.vantrex.skysens.client.gui.config.SimpleConfigEntry;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.hypixel.data.type.GameType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SkysensClient implements ClientModInitializer {

    @Getter
    private static SkysensClient instance;

    private boolean isOnSkyBlock = false;
    private final LocationService locationService;

    // Example config values
    private boolean exampleToggle = true;
    private boolean anotherExampleToggle = false;

    public SkysensClient() {
        instance = this;
        this.locationService = LocationService.getInstance();
    }

    @Override
    public void onInitializeClient() {
        HypixelModAPI api = HypixelModAPI.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();

        api.subscribeToEventPacket(ClientboundLocationPacket.class);
        api.createHandler(ClientboundLocationPacket.class, packet -> {
            // Beispiel: Basisdaten aus dem Paket auslesen
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
            ClientUtil.sendMessage("Server: " + serverName + " | Lobby: " + lobbyName + " | Map: " + map + " | Mode: " + mode);
            this.locationService.handleLocationUpdate(map);

        });

        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("skysens")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        // Schedule the screen opening on the main thread
                        client.send(() -> {
                            client.setScreen(new ConfigScreen(Text.literal("Skysens Config"), createExampleConfig()));
                        });
                        return 1;
                    }));
        });
    }

    private List<ConfigCategory> createExampleConfig() {
        List<ConfigCategory> categories = new ArrayList<>();

        ConfigCategory general = new ConfigCategory("General");
        general.addEntry(new SimpleConfigEntry("Example Toggle", "This is an example boolean toggle.",
                () -> exampleToggle, (val) -> exampleToggle = val));
        general.addEntry(new SimpleConfigEntry("Another Toggle", "Another toggle to test the UI.",
                () -> anotherExampleToggle, (val) -> anotherExampleToggle = val));

        ConfigCategory subCategory = new ConfigCategory("Subcategory");
        subCategory.addEntry(new SimpleConfigEntry("Nested Toggle", "Toggle inside a subcategory.",
                () -> false, (val) -> {}));
        general.addSubCategory(subCategory);

        ConfigCategory visuals = new ConfigCategory("Visuals");
        visuals.addEntry(new SimpleConfigEntry("HUD Enabled", "Show the Skysens HUD.",
                () -> true, (val) -> {}));

        categories.add(general);
        categories.add(visuals);

        return categories;
    }

}
