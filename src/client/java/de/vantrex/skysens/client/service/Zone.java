package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Zone<E extends Enum<E>> {

    private final ZoneEnum<E> zoneEnum;

    ZoneEnum<E> getZoneEnum() {
        return zoneEnum;
    }


    public static <E extends Enum<E>> Zone<E> fromScoreboardLine(String scoreboardLine, SkyblockLocationEnum currentLocation) {
        if (currentLocation == null || currentLocation.getZoneEnum() == null || scoreboardLine == null) {
            return null;
        }
        scoreboardLine = scoreboardLine.trim();
        for (ZoneEnum<?> enumConstant : currentLocation.getZoneEnum().getEnumConstants()) {
            if (scoreboardLine.equals(enumConstant.getScoreboardName())) {
                return new Zone(enumConstant);
            }
        }
        return null;
    }

}
