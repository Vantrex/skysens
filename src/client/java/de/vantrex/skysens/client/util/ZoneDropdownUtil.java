package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.ZoneEnum;

import java.util.Arrays;

public final class ZoneDropdownUtil {
    private ZoneDropdownUtil() {}

    public static String[] zoneNamesFor(SkyblockLocationEnum loc) {
        Class<? extends ZoneEnum<?>> clazz = loc.getZoneEnum();
        if (clazz == null) return new String[] { "(No Zonen)" };

        ZoneEnum<?>[] constants = clazz.getEnumConstants();
        if (constants == null || constants.length == 0) return new String[] { "(No Zone)" };

        return Arrays.stream(constants)
                .map(ZoneEnum::getDisplayName)
                .toArray(String[]::new);
    }
}
