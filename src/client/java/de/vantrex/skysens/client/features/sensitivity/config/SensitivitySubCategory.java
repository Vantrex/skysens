package de.vantrex.skysens.client.features.sensitivity.config;

import de.vantrex.skysens.client.config.annotation.ConfigMasterToggle;
import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.zone.HubZoneEnum;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class SensitivitySubCategory {

    @ConfigOption(name = "Sensitivities per location", desc = "Switch your mouse sensitivity automatically depending on the SkyBlock location and zone you are in. Off means your normal sensitivity is always used")
    @ConfigEditorBoolean
    @ConfigMasterToggle
    @Expose
    public boolean enablePerLocation = false;

    @ConfigOption(name = "Skyblock Location", desc = "The location on Skyblock to use")
    @ConfigEditorDropdown
    public transient Property<SkyblockLocationEnum> locationEnum = Property.of(SkyblockLocationEnum.HUB);

    @ConfigOption(name = "Enable custom sensitivity for location", desc = "Enables custom sensitivity for the selected location")
    @ConfigEditorBoolean
    public transient Property<Boolean> enableForLocation = Property.of(false);

    @ConfigOption(name = "Location Sensitivity", desc = "The sensitivity for the selected location")
    @ConfigEditorSlider(minValue = 1F, maxValue = 100.0F, minStep = 1F)
    public transient Property<Float> locationSensitivity = Property.of(100F);


    @ConfigOption(name = "Zones", desc = "The zone of the selected location to set a sensitivity for")
    @ConfigEditorDynamicZoneDropdown(fieldName = "zones")
    public transient Property<Integer> zones = Property.of(HubZoneEnum.VILLAGE.ordinal());

    @ConfigOption(name = "Enable custom sensitivity for zone", desc = "Enables custom sensitivity for the selected zone")
    @ConfigEditorBoolean
    public transient Property<Boolean> enableForZone = Property.of(false);

    @ConfigOption(name = "Zone Sensitivity", desc = "The sensitivity for the zone")
    @ConfigEditorSlider(minValue = 1F, maxValue = 100.0F, minStep = 1F)
    public transient  Property<Float> zoneSensitivity = Property.of(100F);

}
