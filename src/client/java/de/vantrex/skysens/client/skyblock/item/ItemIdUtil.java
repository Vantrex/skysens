package de.vantrex.skysens.client.skyblock.item;

import java.util.Locale;
import java.util.Map;

/** Turns SkyBlock display names into the parts of item ids, e.g. "Bal's Shard" -> "BALS_SHARD". */
public final class ItemIdUtil {

    private static final Map<Character, Integer> ROMAN = Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100);

    private ItemIdUtil() {
    }

    /** "Ultimate Wise" -> "ULTIMATE_WISE". */
    public static String toId(String name) {
        return name.trim().replace("'", "").replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(Locale.ROOT);
    }

    /** "IV" -> 4. Only I, V, X, L and C. */
    public static int romanToInt(String roman) {
        int total = 0;
        for (int i = 0; i < roman.length(); i++) {
            final int value = ROMAN.get(roman.charAt(i));
            final boolean subtract = i + 1 < roman.length() && ROMAN.get(roman.charAt(i + 1)) > value;
            total += subtract ? -value : value;
        }
        return total;
    }

}
