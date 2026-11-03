package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.party.PartyLeaveReason;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by {@code PartyTracker} when the player is no longer in a party, after the tracker state was cleared. Also fired
 * after a {@link PartyKickEvent}, with {@link PartyLeaveReason#KICKED}.
 */
@Data
public final class PartyLeaveEvent implements SkysensEvent {

    private final @NotNull PartyLeaveReason reason;

}
