package de.vantrex.skysens.client.config.categories.qol.sub;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.HubZoneEnum;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class SensitivitySubCategory {

    @ConfigOption(name = "Sensitivities per location", desc = "Completely disables this feature")
    @ConfigEditorBoolean
    @Expose
    public boolean enablePerLocation = false;

    @ConfigOption(name = "Skyblock Location", desc = "The location on Skyblock to use")
    @ConfigEditorDropdown
    public transient Property<SkyblockLocationEnum> locationEnum = Property.of(SkyblockLocationEnum.HUB);

    @ConfigOption(name = "Enable custom sensitivity for location", desc = "Enables custom sensitivity for the selected location")
    @ConfigEditorBoolean
    public transient Property<Boolean> enableForLocation = Property.of(false);

    @ConfigOption(name = "Location Sensitivity", desc = "The sensitivity for the zone")
    @ConfigEditorSlider(minValue = 1F, maxValue = 100.0F, minStep = 1F)
    public transient Property<Float> locationSensitivity = Property.of(100F);


    @ConfigOption(name = "Zones", desc = "The zones to apply the sensitivity to")
    @ConfigEditorDynamicZoneDropdown(fieldName = "zones")
    public transient Property<Integer> zones = Property.of(HubZoneEnum.VILLAGE.ordinal());

    @ConfigOption(name = "Enable custom sensitivity for zone", desc = "Enables custom sensitivity for the selected zone")
    @ConfigEditorBoolean
    public transient Property<Boolean> enableForZone = Property.of(false);

    @ConfigOption(name = "Zone Sensitivity", desc = "The sensitivity for the zone")
    @ConfigEditorSlider(minValue = 1F, maxValue = 100.0F, minStep = 1F)
    public transient  Property<Float> zoneSensitivity = Property.of(100F);

}
