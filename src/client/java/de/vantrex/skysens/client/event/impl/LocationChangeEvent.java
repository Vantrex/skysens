package de.vantrex.skysens.client.event.impl;

import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public final class LocationChangeEvent implements SkysensEvent {

    private final @Nullable SkyblockLocationEnum oldLocation;
    private final@Nullable SkyblockLocationEnum newLocation;

}
