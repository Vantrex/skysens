package de.vantrex.skysens.client.config;


import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import de.vantrex.skysens.client.config.categories.dev.DevCategory;
import de.vantrex.skysens.client.config.categories.bazaar.BazaarCategory;
import de.vantrex.skysens.client.config.categories.qol.QolCategory;
import de.vantrex.skysens.client.config.gui.GuiOptionEditorDynamicZoneDropdown;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import lombok.NoArgsConstructor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

@NoArgsConstructor
public class SkysensConfig extends Config {


    private static ManagedConfig<SkysensConfig> CONFIG;

    @Category(name = "Bazaar", desc = "Bazaar Options")
    @Expose
    public BazaarCategory bazaarCategory = new BazaarCategory();

    @Category(name = "QOL", desc = "Quality of Life Options")
    @Expose
    public QolCategory qolCategory = new QolCategory();

    @Category(name = "Dev", desc = "Dev Options")
    @Expose
    public DevCategory devCategory = new DevCategory();

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
                            skysensConfigManagedConfigBuilder.customProcessor(ConfigEditorDynamicZoneDropdown.class, (processedOption, configEditorDynamicZoneDropdown) -> {
                                return new GuiOptionEditorDynamicZoneDropdown(processedOption, configEditorDynamicZoneDropdown.fieldName());
                            });
                            return Unit.INSTANCE;
                        }
                    }
            );
        }
        return CONFIG;
    }

}
