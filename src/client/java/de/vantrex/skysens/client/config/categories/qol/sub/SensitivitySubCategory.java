package de.vantrex.skysens.client.config.categories.qol.sub;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.HubZoneEnum;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class SensitivitySubCategory {

    @ConfigOption(name = "Sensitivities per location", desc = "Enables the ability to set sensitivities per location")
    @ConfigEditorBoolean
    @Expose
    public boolean enablePerLocation = false;

    @ConfigOption(name = "Skyblock Location", desc = "The location on skyblock to use")
    @ConfigEditorDropdown
    public Property<SkyblockLocationEnum> locationEnum = Property.of(SkyblockLocationEnum.HUB);

    @ConfigOption(name = "Zones", desc = "The zones to apply the sensitivity to")
    @ConfigEditorDynamicZoneDropdown(fieldName = "zones")
    public Property<Integer> zones = Property.of(HubZoneEnum.VILLAGE.ordinal());

    @ConfigOption(name = "Sensitivity", desc = "The sensitivity of the movement")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 1.0f, minStep = 0.01f)
    public Property<Float> sensitivity = Property.of(1.0f);

}
