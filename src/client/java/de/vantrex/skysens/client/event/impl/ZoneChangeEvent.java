package de.vantrex.skysens.client.event.impl;

import de.vantrex.skysens.client.enums.location.zone.ZoneEnum;
import de.vantrex.skysens.client.event.SkysensEvent;
import de.vantrex.skysens.client.model.Zone;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public final class ZoneChangeEvent implements SkysensEvent {

    private final @Nullable Zone<? extends ZoneEnum<?>> oldZone;
    private final @Nullable Zone<? extends ZoneEnum<?>> newZone;


}
