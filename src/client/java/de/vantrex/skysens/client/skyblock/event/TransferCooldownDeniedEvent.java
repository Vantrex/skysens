package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * Hypixel refused a command with "You may only use this command after Ns on the server!". Fired after the tracker
 * updated its state. {@code command} is the most recently sent command (without "/") if it was sent shortly before,
 * else null.
 */
@Data
public final class TransferCooldownDeniedEvent implements SkysensEvent {

    private final @Nullable String command;

}
