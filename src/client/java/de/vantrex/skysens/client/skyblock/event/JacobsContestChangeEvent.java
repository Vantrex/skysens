package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/** Fired when Jacob's contest starts or ends, or its crop becomes visible; {@code crop} is null if not visible. */
@Data
public final class JacobsContestChangeEvent implements SkysensEvent {

    private final boolean active;
    private final @Nullable String crop;

}
