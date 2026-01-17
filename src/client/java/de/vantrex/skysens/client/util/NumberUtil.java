package de.vantrex.skysens.client.util;

public class NumberUtil {

    public static Double convertFloatToDouble(Float f) {
        if (f == null) return null;
        return Double.valueOf(f);
    }

}
