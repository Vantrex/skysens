package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by {@code PartyTracker} when another player joins the player's party ("ReZuni joined the party."), after they
 * were added to the members.
 */
@Data
public final class PartyMemberJoinEvent implements SkysensEvent {

    private final @NotNull String member;

}
