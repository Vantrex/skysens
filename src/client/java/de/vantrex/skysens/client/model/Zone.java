package de.vantrex.skysens.client.model;

import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.ChatFormatting;

import java.util.Locale;

@RequiredArgsConstructor
@Getter
public class Zone<E extends Enum<E>> {

    private final ZoneEnum<E> zoneEnum;
    private final SkyblockLocationEnum location;

    public static <E extends Enum<E>> Zone<E> fromScoreboardLine(String scoreboardLine, SkyblockLocationEnum location) {
        if (location == null || location.getZoneEnum() == null || scoreboardLine == null) {
            return null;
        }
        final String line = normalize(scoreboardLine);
        if (line.isEmpty()) {
            return null;
        }
        for (ZoneEnum<?> enumConstant : location.getZoneEnum().getEnumConstants()) {
            if (line.equals(normalize(enumConstant.getScoreboardName()))) {
                return new Zone(enumConstant, location);
            }
        }
        return null;
    }

    /**
     * The sidebar line without color codes, the zone symbol ("⏣", "ф") and other symbols Hypixel adds, lower case.
     * "§7⏣ §bRoyal Mines" -> "royal mines".
     */
    private static String normalize(String text) {
        final String stripped = ChatFormatting.stripFormatting(text);
        return (stripped == null ? text : stripped)
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

}
