package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fired by {@code PartyTracker} when the player joins a party ("You have joined ReZuni's party!") or creates one by
 * inviting someone, after the tracker state was updated.
 */
@Data
public final class PartyJoinEvent implements SkysensEvent {

    /** The party leader's name, null if unknown. The player's own name if they created the party. */
    private final @Nullable String leader;
    /** The names of the other members known so far, without the player. */
    private final @NotNull List<String> members;

}
