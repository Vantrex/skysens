package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by {@code CoopMemberTracker} when Hypixel says a co-op member of the current profile joined SkyBlock
 * ("ReZuni joined SkyBlock."), after the cached online status was updated.
 */
@Data
public final class CoopMemberJoinEvent implements SkysensEvent {

    private final @NotNull String member;

}
