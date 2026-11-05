package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/** Fired when the Dwarven/Glacite mining event changes; a null name means no event. */
@Data
public final class MiningEventChangeEvent implements SkysensEvent {

    private final @Nullable String oldEvent;
    private final @Nullable String newEvent;

}
