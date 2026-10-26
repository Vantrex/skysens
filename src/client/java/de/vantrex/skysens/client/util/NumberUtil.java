package de.vantrex.skysens.client.util;

public class NumberUtil {

    public static Double convertFloatToDouble(Float f) {
        if (f == null) return null;
        return Double.valueOf(f);
    }

    /** Short coin format: 950, 12.5k, 160M, 1.25B. */
    public static String formatCoins(double coins) {
        // thresholds sit at the rounding point, so 999,999 becomes "1M" and not "1000k"
        if (coins >= 999_500_000) return trim(coins / 1_000_000_000, 2) + "B";
        if (coins >= 999_500) return trim(coins / 1_000_000, coins >= 99_950_000 ? 0 : 1) + "M";
        if (coins >= 999.5) return trim(coins / 1_000, coins >= 99_950 ? 0 : 1) + "k";
        return trim(coins, 0);
    }

    /** "1,234.5" as a number, the thousands separators removed. */
    public static double parseNumber(String text) {
        return Double.parseDouble(text.replace(",", ""));
    }

    /** "1,234", "12.5k" or "3.5M" as a number. Throws a {@link NumberFormatException} for anything else. */
    public static double parseAbbreviated(String text) {
        final String plain = text.replace(",", "");
        final char unit = Character.toLowerCase(plain.charAt(plain.length() - 1));
        final double factor = unit == 'k' ? 1_000 : unit == 'm' ? 1_000_000 : 1;
        final String number = factor == 1 ? plain : plain.substring(0, plain.length() - 1);
        return Double.parseDouble(number) * factor;
    }

    private static String trim(double value, int decimals) {
        final java.math.BigDecimal rounded = java.math.BigDecimal.valueOf(value)
                .setScale(decimals, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return rounded.toPlainString();
    }

}
