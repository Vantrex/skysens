package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * Fired by {@code PartyTracker} when the player is kicked from their party ("You have been kicked from the party by
 * ReZuni"). A {@link PartyLeaveEvent} with reason {@code KICKED} follows.
 */
@Data
public final class PartyKickEvent implements SkysensEvent {

    /** Who kicked the player, null if unknown. */
    private final @Nullable String kickedBy;

}
