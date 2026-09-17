package de.vantrex.skysens.common.domain.sensitivity;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.common.domain.location.SkyblockLocationEnum;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SensitivityConfiguration {


    @Expose
    private Map<SkyblockLocationEnum, LocationSensitivity> locationSensitivities = new HashMap<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class LocationSensitivity {
        @Expose
        private boolean enabled = false;
        @Expose
        private Float sensitivity = null;
        @Expose
        private Map<String, ZoneSensitivity> zoneSensitivities = new HashMap<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class ZoneSensitivity {
        @Expose
        private boolean enabled = false;
        @Expose
        private Float sensitivity = null;
    }

}
