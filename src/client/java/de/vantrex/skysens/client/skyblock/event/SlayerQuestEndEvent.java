package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/** A slayer quest ended. */
@Data
public final class SlayerQuestEndEvent implements SkysensEvent {

    private final Reason reason;

    public enum Reason {
        COMPLETED,
        FAILED,
        /** The quest left the sidebar without a message: cancelled at Maddox, or lost to a disconnect. */
        CANCELLED
    }

}
