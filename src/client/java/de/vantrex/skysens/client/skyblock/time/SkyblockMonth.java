package de.vantrex.skysens.client.skyblock.time;

import lombok.Getter;

/** The 12 SkyBlock months in calendar order; each season has an early, a middle and a late month. */
@Getter
public enum SkyblockMonth {
    EARLY_SPRING("Early Spring", Season.SPRING),
    SPRING("Spring", Season.SPRING),
    LATE_SPRING("Late Spring", Season.SPRING),
    EARLY_SUMMER("Early Summer", Season.SUMMER),
    SUMMER("Summer", Season.SUMMER),
    LATE_SUMMER("Late Summer", Season.SUMMER),
    EARLY_AUTUMN("Early Autumn", Season.AUTUMN),
    AUTUMN("Autumn", Season.AUTUMN),
    LATE_AUTUMN("Late Autumn", Season.AUTUMN),
    EARLY_WINTER("Early Winter", Season.WINTER),
    WINTER("Winter", Season.WINTER),
    LATE_WINTER("Late Winter", Season.WINTER);

    public enum Season { SPRING, SUMMER, AUTUMN, WINTER }

    private static final SkyblockMonth[] VALUES = values();

    private final String displayName;
    private final Season season;

    SkyblockMonth(String displayName, Season season) {
        this.displayName = displayName;
        this.season = season;
    }

    /** The month by its zero-based index in the year (0 = Early Spring). */
    public static SkyblockMonth ofIndex(int index) {
        return VALUES[index];
    }
}
