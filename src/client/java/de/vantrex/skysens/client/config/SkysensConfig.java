package de.vantrex.skysens.client.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import de.vantrex.skysens.client.config.annotation.ConfigEditorMultiSelect;
import de.vantrex.skysens.client.config.categories.misc.MiscCategory;
import de.vantrex.skysens.client.config.categories.dev.DevCategory;
import de.vantrex.skysens.client.config.migration.ConfigMigrations;
import de.vantrex.skysens.client.features.dungeon.config.DungeonCategory;
import de.vantrex.skysens.client.features.bazaar.config.BazaarCategory;
import de.vantrex.skysens.client.features.fishing.config.FishingCategory;
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

    @Category(name = "Fishing", desc = "Trophy Frog HUD")
    @Expose
    public FishingCategory fishingCategory = new FishingCategory();

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

    @Category(name = "Misc", desc = "Year of the Witch ingredients, Crimson Isle volcano eruptions")
    @Expose
    public MiscCategory miscCategory = new MiscCategory();

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
            final File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "skysens/config.json");
            final ConfigMigrations migrations = ConfigMigrations.read(configFile);
            CONFIG = ManagedConfig.create(
                    configFile,
                    SkysensConfig.class,
                    new Function1<ManagedConfigBuilder<SkysensConfig>, Unit>() {
                        @Override
                        public Unit invoke(ManagedConfigBuilder<SkysensConfig> skysensConfigManagedConfigBuilder) {
                            skysensConfigManagedConfigBuilder.customProcessor(ConfigEditorDynamicZoneDropdown.class,
                                    (processedOption, configEditorDynamicZoneDropdown)
                                            -> new ZoneDropdownEditorStub(processedOption));
                            skysensConfigManagedConfigBuilder.customProcessor(ConfigEditorMultiSelect.class,
                                    (processedOption, configEditorMultiSelect)
                                            -> new MultiSelectEditorStub(processedOption));
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
            if (migrations.apply(CONFIG.getInstance())) {
                CONFIG.saveToFile();
            }
        }
        return CONFIG;
    }

}
