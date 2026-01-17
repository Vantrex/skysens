package de.vantrex.skysens.client.model;

import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Zone<E extends Enum<E>> {

    private final ZoneEnum<E> zoneEnum;
    private final SkyblockLocationEnum location;

    public static <E extends Enum<E>> Zone<E> fromScoreboardLine(String scoreboardLine, SkyblockLocationEnum location) {
        if (location == null || location.getZoneEnum() == null || scoreboardLine == null) {
            return null;
        }
        scoreboardLine = scoreboardLine.trim();
        for (ZoneEnum<?> enumConstant : location.getZoneEnum().getEnumConstants()) {
            if (scoreboardLine.equals(enumConstant.getScoreboardName())) {
                return new Zone(enumConstant, location);
            }
        }
        return null;
    }

}
