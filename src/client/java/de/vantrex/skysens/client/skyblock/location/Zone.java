package de.vantrex.skysens.client.skyblock.location;

import de.vantrex.skysens.client.skyblock.location.zone.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.ChatFormatting;

import java.text.Normalizer;
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
     * "§7⏣ §bRoyal Mines" -> "royal mines". Accents are dropped from their letter, "Château" -> "chateau".
     */
    private static String normalize(String text) {
        final String stripped = ChatFormatting.stripFormatting(text);
        return Normalizer.normalize(stripped == null ? text : stripped, Normalizer.Form.NFD)
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

}
