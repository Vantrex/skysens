package de.vantrex.skysens.client.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.vantrex.skysens.client.config.SensitivityConfiguration;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.qol.sub.SensitivitySubCategory;
import de.vantrex.skysens.client.enums.location.zone.ZoneEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Getter
public class SensitivityService {

    @Getter
    private static final SensitivityService instance = new SensitivityService();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final SensitivitySubCategory category = SkysensConfig.getInstance().qolCategory.sensitivity;
    private final SensitivityConfiguration sensitivityConfiguration;

    public SensitivityService() {
        this.sensitivityConfiguration = this.loadConfiguration();
    }

    public void initConfigHooks() {
        category.locationEnum.whenChanged((oldValue, newValue) -> {
            if (oldValue == newValue) {
                return;
            }
            category.zones.accept(getFirstZoneEnumConstant(newValue.getZoneEnum()).ordinal());
        });
    }


    private <T extends Enum<?>> T getFirstZoneEnumConstant(Class<?> zoneEnumClass) {
        return (T) zoneEnumClass.getEnumConstants()[0];
    }

    private SensitivityConfiguration loadConfiguration() {
        final var jsonFile = FabricLoader.getInstance().getConfigDir().resolve("skysens/sensitivity.json");
        if (!Files.exists(jsonFile)) {
            return new SensitivityConfiguration();
        }
        try {
            return GSON.fromJson(Files.readString(jsonFile), SensitivityConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
