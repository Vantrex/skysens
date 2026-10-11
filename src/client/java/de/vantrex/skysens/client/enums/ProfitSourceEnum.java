package de.vantrex.skysens.client.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Which trackers {@code !profit} reports. */
@Getter
@RequiredArgsConstructor
public enum ProfitSourceEnum {

    /** SkyHanni's trackers when SkyHanni is installed, SkySens' own tracker otherwise. */
    SKYHANNI("SkyHanni (if installed)"),
    SKYSENS("SkySens");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }

}
