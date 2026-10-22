package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public final class LocationChangeEvent implements SkysensEvent {

    private final @Nullable SkyblockLocationEnum oldLocation;
    private final @Nullable SkyblockLocationEnum newLocation;

}
