package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.party.PartyLeaveReason;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by {@code PartyTracker} when another member leaves the player's party, is kicked or is removed for being
 * offline, after they were removed from the members. Not fired for each member when the party is disbanded.
 */
@Data
public final class PartyMemberLeaveEvent implements SkysensEvent {

    private final @NotNull String member;
    /** {@code LEFT}, {@code KICKED} or {@code DISCONNECTED}. */
    private final @NotNull PartyLeaveReason reason;

}
