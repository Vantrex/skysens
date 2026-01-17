package de.vantrex.skysens.client.feature.qol.sensitivity;

import de.vantrex.skysens.client.config.SensitivityConfiguration;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.event.SkysensListener;
import de.vantrex.skysens.client.event.impl.ZoneChangeEvent;
import de.vantrex.skysens.client.feature.MouseFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.service.SensitivityService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import net.minecraft.sound.SoundEvents;

@SkySensFeature
public class ZoneBasedSensitivityFeature implements MouseFeature, SkysensListener<ZoneChangeEvent> {

    private final SensitivityConfiguration sensitivityConfiguration = SensitivityService.getInstance().getConfig();

    private Double currentSensitivity;

    public ZoneBasedSensitivityFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this, ZoneChangeEvent.class);
    }

    @Override
    public void handle(ZoneChangeEvent event) {
        if (event.getNewZone() == null) {
            if (this.currentSensitivity != null) {
                ClientUtil.sendTitle("§5SkySens", "Reset Sensitivity ");
                ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            }
            this.currentSensitivity = null;
            return;
        }
        if (this.sensitivityConfiguration == null) {
            if (this.currentSensitivity != null) {
                ClientUtil.sendTitle("§5SkySens", "Reset Sensitivity ");
                ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            }
            this.currentSensitivity = null;
            return;
        }
        final var locationConfig = this.sensitivityConfiguration.getLocationSensitivities().get(event.getNewZone().getLocation());
        if (locationConfig == null) {
            if (this.currentSensitivity != null) {
                ClientUtil.sendTitle("§5SkySens", "Reset Sensitivity ");
                ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            }
            this.currentSensitivity = null;
            return;
        }
        SensitivityConfiguration.ZoneSensitivity zoneSensitivity = locationConfig.getZoneSensitivities().get(event.getNewZone().getZoneEnum().name());
        if (zoneSensitivity == null || !zoneSensitivity.isEnabled()) {
            if (this.currentSensitivity != null) {
                ClientUtil.sendTitle("§5SkySens", "Reset Sensitivity ");
                ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            }
            this.currentSensitivity = null;
            return;
        }
        final var newSensitivity = NumberUtil.convertFloatToDouble(zoneSensitivity.getSensitivity());
        if (this.currentSensitivity != null && this.currentSensitivity.equals(newSensitivity)) {
            return;
        }
        ClientUtil.sendTitle(LocationService.getZoneName(event.getNewZone()), "Sensitivity: " + locationConfig.getSensitivity() * 100F);
        ClientUtil.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
        this.currentSensitivity = newSensitivity;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().qolCategory.sensitivity.enablePerLocation;
    }

    @Override
    public Double getMouseSensitivityScale() {
        return this.currentSensitivity;
    }

}