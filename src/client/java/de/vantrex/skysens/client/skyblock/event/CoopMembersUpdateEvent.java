package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Fired by {@code CoopMemberTracker} when the co-op widget in the tab list on the private island shows other members
 * or online statuses than were cached. Both exclude the player.
 */
@Data
public final class CoopMembersUpdateEvent implements SkysensEvent {

    /** The names of the other co-op members. */
    private final @NotNull List<String> members;
    /** The names of the members that are online. */
    private final @NotNull Set<String> online;

}
