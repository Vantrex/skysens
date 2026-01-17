package de.vantrex.skysens.client.feature.qol.sensitivity;

import de.vantrex.skysens.client.config.SensitivityConfiguration;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.event.SkysensListener;
import de.vantrex.skysens.client.event.impl.LocationChangeEvent;
import de.vantrex.skysens.client.feature.MouseFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.service.SensitivityService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.sound.SoundEvents;

@SkySensFeature
@Slf4j
public class LocationBasedSensitivityFeature implements MouseFeature, SkysensListener<LocationChangeEvent> {

    private final SensitivityConfiguration sensitivityConfiguration = SensitivityService.getInstance().getConfig();
    private Double currentSensitivity;

    public LocationBasedSensitivityFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this, LocationChangeEvent.class);
    }

    @Override
    public void handle(LocationChangeEvent event) {
        log.info("Location changed to {}", event.getNewLocation());
        if (this.sensitivityConfiguration == null) {
            log.warn("No sensitivity configuration found, disabling sensitivity feature");
            if (this.currentSensitivity != null) {
                ClientUtil.sendTitle("§5SkySens", "Reset Sensitivity ");
                ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            }
            this.currentSensitivity = null;
            return;
        }
        final var locationConfig = this.sensitivityConfiguration.getLocationSensitivities().get(event.getNewLocation());
        if (locationConfig == null || !locationConfig.isEnabled()) {
            if (this.currentSensitivity != null) {
                ClientUtil.sendTitle("§5SkySens", "Reset Sensitivity ");
                ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            }
            log.info("No sensitivity config found for location {} or it is disabled, disabling sensitivity feature", event.getNewLocation());
            this.currentSensitivity = null;
            return;
        }
        log.info("Using sensitivity config for location {}: {}", event.getNewLocation(), locationConfig);
        final var newSensitivity = NumberUtil.convertFloatToDouble(locationConfig.getSensitivity());
        if (this.currentSensitivity != null && this.currentSensitivity.equals(newSensitivity)) {
            return;
        }
        ClientUtil.sendTitle(LocationService.getLocationName(event.getNewLocation()), "Sensitivity: " + locationConfig.getSensitivity() * 100F);
        ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
        this.currentSensitivity = newSensitivity;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().qolCategory.sensitivity.enablePerLocation;
    }

    @Override
    public Double getMouseSensitivityScale() {
        //log.info("Returning location sensitivity scale {}", this.currentSensitivity);
        return this.currentSensitivity;
    }

    @Override
    public int priority() {
        return 1;
    }
}
