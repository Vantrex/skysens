package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;

/** Fired when the Kuudra tier (1-5) shown in the sidebar changes; {@code tier} is 0 when leaving Kuudra. */
@Data
public final class KuudraTierChangeEvent implements SkysensEvent {

    private final int tier;

}
