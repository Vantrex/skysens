package de.vantrex.skysens.common.util;

public class NumberUtil {

    public static Double convertFloatToDouble(Float f) {
        if (f == null) return null;
        return Double.valueOf(f);
    }

}
