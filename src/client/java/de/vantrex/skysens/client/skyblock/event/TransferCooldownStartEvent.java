package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/** A transfer cooldown started or was extended; it ends at {@code endsAtMs} (epoch milliseconds). */
@Data
public final class TransferCooldownStartEvent implements SkysensEvent {

    private final long endsAtMs;

}
