package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.skyblock.location.zone.ZoneEnum;
import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.location.Zone;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public final class ZoneChangeEvent implements SkysensEvent {

    private final @Nullable Zone<? extends ZoneEnum<?>> oldZone;
    private final @Nullable Zone<? extends ZoneEnum<?>> newZone;


}
