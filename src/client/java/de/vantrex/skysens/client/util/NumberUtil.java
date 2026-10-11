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

    private static String trim(double value, int decimals) {
        final java.math.BigDecimal rounded = java.math.BigDecimal.valueOf(value)
                .setScale(decimals, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return rounded.toPlainString();
    }

}
