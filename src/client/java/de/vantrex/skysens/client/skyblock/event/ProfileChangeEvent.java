package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/** Fired by {@code ProfileService} when the SkyBlock profile changes; null while it is unknown (e.g. after leaving). */
@Data
public final class ProfileChangeEvent implements SkysensEvent {

    private final @Nullable String oldProfile;
    private final @Nullable String newProfile;

}
