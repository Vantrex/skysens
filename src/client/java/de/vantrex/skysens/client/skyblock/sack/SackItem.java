package de.vantrex.skysens.client.skyblock.sack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One item line of a sack message: "+27 Gabagool Distillate (Combat Sack)".
 *
 * @param name   the item name, e.g. "Gabagool Distillate"
 * @param amount how many were added or removed, always positive
 * @param sack   the sack it went into or came out of, e.g. "Combat Sack", null if the line didn't name one
 */
public record SackItem(@NotNull String name, int amount, @Nullable String sack) {
}
