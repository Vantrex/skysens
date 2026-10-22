package de.vantrex.skysens.client.features.sensitivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.sensitivity.config.SensitivitySubCategory;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.zone.ZoneEnum;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Slf4j
@Getter
public class SensitivityService {

    @Getter
    private static final SensitivityService instance = new SensitivityService();

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final SensitivitySubCategory category = SkysensConfig.getInstance().qolCategory.sensitivity;
    private final SensitivityConfiguration config;

    public SensitivityService() {
        this.config = this.loadConfiguration();
        this.saveConfiguration();
    }

    public void initConfigHooks() {
        category.locationEnum.whenChanged((oldValue, newValue) -> {
            if (oldValue == newValue) {
                return;
            }
            category.zones.set(Optional.ofNullable(newValue.getZoneEnum()).map(aClass -> getFirstZoneEnumConstant(aClass).ordinal()).orElse(0));
            category.zones.notifyObservers();
            SensitivityConfiguration.LocationSensitivity locationSensitivity = config.getLocationSensitivities().get(newValue);
            ClientUtil.sendDebug("Switched to location " + newValue.name() + " with sensitivity config: " + locationSensitivity);
            if (locationSensitivity == null) {
                category.enableForLocation.set(false);
                category.locationSensitivity.set(this.translatedMinecraftClientSensitivity());
                category.locationEnum.notifyObservers();
                category.enableForLocation.notifyObservers();
                return;
            }
            category.locationSensitivity.set(this.convertFloatSensitivityToIntegerLikeFloat(locationSensitivity.getSensitivity()));
            category.enableForLocation.set(locationSensitivity.isEnabled());
        });
        category.enableForLocation.whenChanged((oldValue, newValue) -> {
            SkyblockLocationEnum skyblockLocationEnum = category.locationEnum.get();
            if (skyblockLocationEnum == null) {
                return;
            }
            SensitivityConfiguration.LocationSensitivity locationSensitivity = config.getLocationSensitivities().computeIfAbsent(skyblockLocationEnum, location -> new SensitivityConfiguration.LocationSensitivity());
            locationSensitivity.setEnabled(newValue);
            this.saveConfiguration();
        });
        category.locationSensitivity.whenChanged((oldValue, newValue) -> {
            SkyblockLocationEnum skyblockLocationEnum = category.locationEnum.get();
            if (skyblockLocationEnum == null) {
                return;
            }
            SensitivityConfiguration.LocationSensitivity locationSensitivity = config.getLocationSensitivities()
                    .computeIfAbsent(skyblockLocationEnum, location -> new SensitivityConfiguration.LocationSensitivity());
            locationSensitivity.setSensitivity(this.convertIntegerLikeFloatToSensitivityFloat(newValue));
            this.saveConfiguration();
        });
        category.zones.whenChanged((oldValue, newValue) -> {
            SkyblockLocationEnum skyblockLocationEnum = category.locationEnum.get();
            if (skyblockLocationEnum == null) {
                return;
            }
            SensitivityConfiguration.LocationSensitivity locationSensitivity = config.getLocationSensitivities()
                    .computeIfAbsent(skyblockLocationEnum, location -> new SensitivityConfiguration.LocationSensitivity());
            Class<?> zoneEnumClass = skyblockLocationEnum.getZoneEnum();
            if (zoneEnumClass == null) {
                category.enableForZone.set(false);
                category.zoneSensitivity.set(this.translatedMinecraftClientSensitivity());
                return;
            }
            Object[] zoneConstants = zoneEnumClass.getEnumConstants();
            if (newValue == null || newValue < 0 || newValue >= zoneConstants.length) {
                return;
            }
            Enum<? extends ZoneEnum<?>> enumConstant = (Enum<? extends ZoneEnum<?>>) zoneConstants[newValue];
            locationSensitivity.getZoneSensitivities().computeIfAbsent(enumConstant.name(), zone -> new SensitivityConfiguration.ZoneSensitivity(false, this.translatedMinecraftClientSensitivity()));
            SensitivityConfiguration.ZoneSensitivity zoneSensitivity = locationSensitivity.getZoneSensitivities().get(enumConstant.name());
            category.enableForZone.set(zoneSensitivity.isEnabled());
            category.zoneSensitivity.set(this.convertFloatSensitivityToIntegerLikeFloat(zoneSensitivity.getSensitivity()));
        });

        category.enableForZone.whenChanged((oldValue, newValue) -> {
            SkyblockLocationEnum skyblockLocationEnum = category.locationEnum.get();
            if (skyblockLocationEnum == null) {
                return;
            }
            SensitivityConfiguration.LocationSensitivity locationSensitivity = config.getLocationSensitivities()
                    .computeIfAbsent(skyblockLocationEnum, location -> new SensitivityConfiguration.LocationSensitivity());
            Class<?> zoneEnumClass = skyblockLocationEnum.getZoneEnum();
            if (zoneEnumClass == null) {
                return;
            }
            Object[] zoneConstants = zoneEnumClass.getEnumConstants();
            Integer zoneIndex = category.zones.get();
            if (zoneIndex == null || zoneIndex < 0 || zoneIndex >= zoneConstants.length) {
                return;
            }
            Enum<? extends ZoneEnum<?>> enumConstant = (Enum<? extends ZoneEnum<?>>) zoneConstants[zoneIndex];
            locationSensitivity.getZoneSensitivities().computeIfAbsent(enumConstant.name(), zone -> new SensitivityConfiguration.ZoneSensitivity(false, this.translatedMinecraftClientSensitivity()))
                    .setEnabled(newValue);
            this.saveConfiguration();
        });
        category.zoneSensitivity.whenChanged((oldValue, newValue) -> {
            SkyblockLocationEnum skyblockLocationEnum = category.locationEnum.get();
            if (skyblockLocationEnum == null) {
                return;
            }
            SensitivityConfiguration.LocationSensitivity locationSensitivity = config.getLocationSensitivities()
                    .computeIfAbsent(skyblockLocationEnum, location -> new SensitivityConfiguration.LocationSensitivity());
            Class<?> zoneEnumClass = skyblockLocationEnum.getZoneEnum();
            if (zoneEnumClass == null) {
                return;
            }
            Object[] zoneConstants = zoneEnumClass.getEnumConstants();
            Integer zoneIndex = category.zones.get();
            if (zoneIndex == null || zoneIndex < 0 || zoneIndex >= zoneConstants.length) {
                return;
            }
            Enum<? extends ZoneEnum<?>> enumConstant = (Enum<? extends ZoneEnum<?>>) zoneConstants[zoneIndex];
            locationSensitivity.getZoneSensitivities().computeIfAbsent(enumConstant.name(), zone -> new SensitivityConfiguration.ZoneSensitivity(false, this.translatedMinecraftClientSensitivity()))
                    .setSensitivity(this.convertIntegerLikeFloatToSensitivityFloat(newValue));
            this.saveConfiguration();
        });
    }


    private <T extends Enum<?>> T getFirstZoneEnumConstant(Class<?> zoneEnumClass) {
        if (zoneEnumClass == null) {
            return null;
        }
        return (T) zoneEnumClass.getEnumConstants()[0];
    }

    private void saveConfiguration() {
        try {
            try (var writer = Files.newBufferedWriter(FabricLoader.getInstance().getConfigDir().resolve("skysens/sensitivity.json"))) {
                gson.toJson(this.config, writer);
            }
        } catch (IOException e) {
            log.error("Failed to save sensitivity configuration", e);
        }

    }

    private Float convertFloatSensitivityToIntegerLikeFloat(Float sensitivity) {
        if (sensitivity == null) {
            return (float) (int)(Minecraft.getInstance().options.sensitivity().get().floatValue() * 100);
        }
        return (float) (int) (sensitivity * 100);
    }

    private Float convertIntegerLikeFloatToSensitivityFloat(Float sensitivity) {
        return sensitivity / 100F;
    }

    private SensitivityConfiguration loadConfiguration() {
        final var jsonFile = FabricLoader.getInstance().getConfigDir().resolve("skysens/sensitivity.json");
        if (!Files.exists(jsonFile)) {
            return new SensitivityConfiguration();
        }
        try {
            return gson.fromJson(Files.readString(jsonFile), SensitivityConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private float translatedMinecraftClientSensitivity() {
        return this.convertFloatSensitivityToIntegerLikeFloat(Minecraft.getInstance().options.sensitivity().get().floatValue());
    }
    
}
