package de.vantrex.skysens.client.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.features.visuals.config.SwingAnimationAccordion;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import de.vantrex.skysens.client.config.categories.dev.DevCategory;
import de.vantrex.skysens.client.features.dungeon.config.DungeonCategory;
import de.vantrex.skysens.client.features.bazaar.config.BazaarCategory;
import de.vantrex.skysens.client.chatcommand.config.ChatCommandsCategory;
import de.vantrex.skysens.client.config.categories.general.GeneralCategory;
import de.vantrex.skysens.client.features.mining.config.MiningCategory;
import de.vantrex.skysens.client.config.categories.qol.QolCategory;
import de.vantrex.skysens.client.features.slayer.config.SlayerCategory;
import de.vantrex.skysens.client.features.visuals.config.VisualsCategory;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder;
import kotlin.ExperimentalStdlibApi;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import lombok.NoArgsConstructor;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@NoArgsConstructor
public class SkysensConfig extends Config {


    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");
    private static ManagedConfig<SkysensConfig> CONFIG;

    @Category(name = "General", desc = "Notifications and the profit tracker")
    @Expose
    public GeneralCategory generalCategory = new GeneralCategory();

    @Category(name = "Bazaar", desc = "Order highlights, alerts and click protection")
    @Expose
    public BazaarCategory bazaarCategory = new BazaarCategory();

    @Category(name = "Quality of Life", desc = "Sensitivity per location and junk highlighting")
    @Expose
    public QolCategory qolCategory = new QolCategory();

    @Category(name = "Visuals", desc = "Entity scaling, held item and camera options")
    @Expose
    public VisualsCategory visualsCategory = new VisualsCategory();

    @Category(name = "Mining", desc = "Mining notifications")
    @Expose
    public MiningCategory miningCategory = new MiningCategory();

    @Category(name = "Dungeon", desc = "Dungeon and Kuudra reward chests")
    @Expose
    public DungeonCategory dungeonCategory = new DungeonCategory();

    @Category(name = "Chat Commands", desc = "Commands others can use in co-op and party chat")
    @Expose
    public ChatCommandsCategory chatCommandsCategory = new ChatCommandsCategory();

    @Category(name = "Slayer", desc = "Slayer session stats, shown by !slayer")
    @Expose
    public SlayerCategory slayerCategory = new SlayerCategory();
    @Category(name = "Chat Filter", desc = "Hide spammy chat messages one by one")
    @Expose
    public de.vantrex.skysens.client.features.chatfilter.config.ChatFilterCategory chatFilterCategory = new de.vantrex.skysens.client.features.chatfilter.config.ChatFilterCategory();

    @Category(name = "Dev", desc = "Debugging and diagnostics")
    @Expose
    public DevCategory devCategory = new DevCategory();

    /**
     * Id of the selected config GUI theme (see ThemePreset). Deliberately has no @Category / @ConfigOption:
     * MoulConfig's processor only inspects fields annotated with @Category, so this stays out of the option
     * tree, while @Expose makes Gson persist it in config.json.
     */
    @Expose
    public String guiTheme = "MIDNIGHT";

    /** Selected config GUI font: a FontFamily id (default INTER, unknown values fall back to INTER). Same persistence approach as {@link #guiTheme}. */
    @Expose
    public String guiFont = "INTER";

    /** Config GUI scale mode: "AUTO" (default, a smaller effective scale when the game's is too large) or "GAME" (the game's GUI scale). Same persistence approach as {@link #guiTheme}. */
    @Expose
    public String guiScaleMode = "AUTO";

    @Override
    public void saveNow() {
        super.saveNow();
        CONFIG.saveToFile();
    }

    @Override
    public StructuredText getTitle() {
        return StructuredText.of("§5§lSky§d§lSens");
    }

    public static SkysensConfig getInstance() {
        return CONFIG.getInstance();
    }

    public static ManagedConfig<SkysensConfig> getConfig() {
        if (CONFIG == null) {
            final JsonObject legacySwing = readLegacySwingSettings();
            CONFIG = ManagedConfig.create(
                    new File(FabricLoader.getInstance().getConfigDir().toFile(), "skysens/config.json"),
                    SkysensConfig.class,
                    new Function1<ManagedConfigBuilder<SkysensConfig>, Unit>() {
                        @Override
                        public Unit invoke(ManagedConfigBuilder<SkysensConfig> skysensConfigManagedConfigBuilder) {
                            skysensConfigManagedConfigBuilder.customProcessor(ConfigEditorDynamicZoneDropdown.class,
                                    (processedOption, configEditorDynamicZoneDropdown)
                                            -> new ZoneDropdownEditorStub(processedOption));
                            // MoulConfig keeps the defaults when parsing fails, and the next save would
                            // overwrite the user's file. Keep a copy of the unreadable file first.
                            skysensConfigManagedConfigBuilder.setLoadFailed((dataFile, exception) -> {
                                File file = dataFile.getFile();
                                File backup = new File(file.getParentFile(),
                                        file.getName() + ".broken-" + System.currentTimeMillis());
                                try {
                                    Files.copy(file.toPath(), backup.toPath());
                                    LOGGER.error("Could not load {}, kept a copy at {}", file, backup, exception);
                                } catch (IOException copyException) {
                                    LOGGER.error("Could not load {} or back it up", file, exception);
                                }
                            });
                            return Unit.INSTANCE;
                        }
                    }
            );
            if (legacySwing != null) {
                migrateLegacySwingSettings(legacySwing);
            }
        }
        return CONFIG;
    }

    /** Returns the pre-move qolCategory.swingAnimation object if the file still has it and no visuals swing group yet. */
    private static JsonObject readLegacySwingSettings() {
        final File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "skysens/config.json");
        if (!file.isFile()) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            final JsonObject visuals = root.has("visualsCategory") && root.get("visualsCategory").isJsonObject()
                    ? root.getAsJsonObject("visualsCategory") : null;
            if (visuals != null && visuals.has("swingAnimation")) {
                return null;
            }
            final JsonElement qol = root.get("qolCategory");
            if (qol != null && qol.isJsonObject() && qol.getAsJsonObject().get("swingAnimation") instanceof JsonObject swing) {
                return swing;
            }
        } catch (Exception e) {
            LOGGER.warn("Could not inspect config for legacy swing settings", e);
        }
        return null;
    }

    private static void migrateLegacySwingSettings(JsonObject legacySwing) {
        try {
            final SwingAnimationAccordion old = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                    .fromJson(legacySwing, SwingAnimationAccordion.class);
            old.clamp();
            CONFIG.getInstance().visualsCategory.swingAnimation.copyFrom(old);
            CONFIG.saveToFile();
            LOGGER.info("Migrated swing animation settings from QOL to Visuals");
        } catch (Exception e) {
            LOGGER.warn("Failed to migrate legacy swing settings", e);
        }
    }

}
