package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.Zone;
import org.jetbrains.annotations.Nullable;

/** Island and zone. Null means not known (yet). */
public final class LocationFacts {

    public @Nullable SkyblockLocationEnum island() {
        return LocationService.getInstance().getCurrentLocation();
    }

    public @Nullable Zone<?> zone() {
        return LocationService.getInstance().getCurrentZone();
    }

}
