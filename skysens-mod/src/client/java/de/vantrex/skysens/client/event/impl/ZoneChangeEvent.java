package de.vantrex.skysens.client.event.impl;

import de.vantrex.skysens.common.domain.location.zone.ZoneEnum;
import de.vantrex.skysens.client.event.SkysensEvent;
import de.vantrex.skysens.common.domain.location.Zone;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public final class ZoneChangeEvent implements SkysensEvent {

    private final @Nullable Zone<? extends ZoneEnum<?>> oldZone;
    private final @Nullable Zone<? extends ZoneEnum<?>> newZone;


}
