package de.vantrex.skysens.client.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.ZoneEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SensitivityConfiguration {

    @Expose
    private Map<SkyblockLocationEnum, Sensitivity> locationSensitivities = new HashMap<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Sensitivity {
        @Expose
        private Float sensitivity = null;
        @Expose
        private Map<String, Float> zoneSensitivities = new HashMap<>();
    }

}
