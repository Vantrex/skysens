package de.vantrex.skysens.client.skyblock.price;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;

/** A source of item prices. Empty results make {@link ItemPriceService} try the next provider. */
public interface ItemPriceProvider {

    /** Display name, used for debug output. */
    @NotNull String name();

    /** Whether this provider can be used at all, e.g. whether the backing mod is installed. */
    boolean isAvailable();

    /** Price of one item, looked up by its display name, e.g. "Judgement Core" or "Legendary Ender Dragon". */
    @NotNull OptionalDouble price(@NotNull String itemName);

    /** Makes sure the price data is loaded and not too old. Must not block. */
    default void refresh() {
    }

}
