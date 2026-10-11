package de.vantrex.skysens.client.config;


import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import de.vantrex.skysens.client.config.categories.dev.DevCategory;
import de.vantrex.skysens.client.config.categories.bazaar.BazaarCategory;
import de.vantrex.skysens.client.config.categories.general.GeneralCategory;
import de.vantrex.skysens.client.config.categories.mining.MiningCategory;
import de.vantrex.skysens.client.config.categories.qol.QolCategory;
import de.vantrex.skysens.client.config.categories.visuals.VisualsCategory;
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

    @Category(name = "General", desc = "General Options")
    @Expose
    public GeneralCategory generalCategory = new GeneralCategory();

    @Category(name = "Bazaar", desc = "Bazaar Options")
    @Expose
    public BazaarCategory bazaarCategory = new BazaarCategory();

    @Category(name = "QOL", desc = "Quality of Life Options")
    @Expose
    public QolCategory qolCategory = new QolCategory();

    @Category(name = "Visuals", desc = "Entity scaling, held item and camera options")
    @Expose
    public VisualsCategory visualsCategory = new VisualsCategory();

    @Category(name = "Mining", desc = "Mining Options")
    @Expose
    public MiningCategory miningCategory = new MiningCategory();

    @Category(name = "Dev", desc = "Dev Options")
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
        }
        return CONFIG;
    }

}
