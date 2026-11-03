package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.party.PartyRole;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired by {@code PartyTracker} when a party member, or the player, is promoted or demoted, or the party is transferred
 * ("ReZuni has promoted FaiN69 to Party Moderator"), after the tracker state was updated. A transfer fires once for the
 * new leader and once for the old one if they stay in the party. Ranks re-read from the Mod API or {@code /party list}
 * only fire a {@link PartyMembersUpdateEvent}.
 */
@Data
public final class PartyRoleChangeEvent implements SkysensEvent {

    private final @NotNull String member;
    /** Whether {@link #member} is the player. */
    private final boolean self;
    /** Null if the old rank was unknown. */
    private final @Nullable PartyRole oldRole;
    private final @NotNull PartyRole newRole;

}
