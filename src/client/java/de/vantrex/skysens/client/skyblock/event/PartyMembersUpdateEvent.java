package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.party.PartyRole;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fired by {@code PartyTracker} when the whole party was re-read, from the Hypixel Mod API or the {@code /party list}
 * output, and differs from what was tracked, ranks included. Join, leave and rank change events are fired separately
 * for single changes.
 */
@Data
public final class PartyMembersUpdateEvent implements SkysensEvent {

    private final boolean inParty;
    /** The player's own rank, null if unknown or not in a party. */
    private final @Nullable PartyRole role;
    /** The leader's name, null if unknown or not in a party. */
    private final @Nullable String leader;
    /** The names of the other members, without the player and without members whose name is unknown. */
    private final @NotNull List<String> members;
    /** The party size including the player, 0 if not in a party. */
    private final int size;

}
