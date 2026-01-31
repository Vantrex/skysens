package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import org.jetbrains.annotations.Nullable;

public final class LocationUtil {

    private LocationUtil() {
    }

    public static boolean isOnMiningRelatedIsland(final @Nullable SkyblockLocationEnum location) {
        if (location == null) return false;
        switch (location) {
            case DWARVEN_MINES, GOLD_MINE, DEEP_CAVERNS, CRYSTAL_HOLLOWS, THE_END -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
