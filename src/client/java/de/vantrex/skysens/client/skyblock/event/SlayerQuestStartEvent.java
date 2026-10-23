package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/**
 * A slayer quest started. {@code startSeen} is false when the quest was only noticed later (boss spawn message, or
 * the sidebar after joining), so its start time is unknown.
 */
@Data
public final class SlayerQuestStartEvent implements SkysensEvent {

    private final boolean startSeen;

}
