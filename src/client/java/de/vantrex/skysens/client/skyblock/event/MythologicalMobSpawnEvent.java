package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.diana.MythologicalMob;
import lombok.Data;

/** Fired by {@code DianaTracker} when a burrow spawns a mythological mob. */
@Data
public final class MythologicalMobSpawnEvent implements SkysensEvent {

    private final MythologicalMob mob;
    private final boolean rare;
}
