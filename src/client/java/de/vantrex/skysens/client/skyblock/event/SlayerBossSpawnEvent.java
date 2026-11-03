package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/** The boss of the running slayer quest spawned, e.g. "Inferno Demonlord IV" (null if the name wasn't readable). */
@Data
public final class SlayerBossSpawnEvent implements SkysensEvent {

    private final @Nullable String boss;

}
