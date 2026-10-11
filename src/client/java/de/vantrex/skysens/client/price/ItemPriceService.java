package de.vantrex.skysens.client.price;

import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.ItemIdUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Item prices. Uses SkyHanni when it is installed and works, otherwise SkySens' own price data.
 */
public final class ItemPriceService {

    @Getter
    private static final ItemPriceService instance = new ItemPriceService();

    private final HypixelPriceProvider hypixel = new HypixelPriceProvider();
    private final List<ItemPriceProvider> providers = List.of(
            new SkyHanniPriceProvider(),
            this.hypixel
    );

    private ItemPriceService() {
    }

    /** Price of one item by display name, or empty if no provider knows it (yet). */
    public @NotNull OptionalDouble getPrice(@NotNull String itemName) {
        for (final ItemPriceProvider provider : this.providers) {
            if (!provider.isAvailable()) {
                continue;
            }
            final OptionalDouble price = provider.price(itemName);
            if (price.isPresent()) {
                ClientUtil.sendDebug("Price of " + itemName + " from " + provider.name() + ": " + price.getAsDouble());
                return price;
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * Price by item id, from SkySens' own price data (Hypixel bazaar ids, NEU lowest BIN ids). Used for
     * items that have no unique display name, like enchanted books and essence. Loads the data if needed.
     */
    public @NotNull OptionalDouble getPriceById(@NotNull String id) {
        return this.hypixel.priceById(id);
    }

    /** Price of an enchanted book, e.g. ("Smoldering", 1); ultimate enchants included. Uses SkySens' own price data. */
    public @NotNull OptionalDouble getBookPrice(@NotNull String enchant, int level) {
        final String id = ItemIdUtil.toId(enchant) + "_" + level;
        final OptionalDouble ultimate = this.getPriceById("ENCHANTMENT_ULTIMATE_" + id);
        return ultimate.isPresent() ? ultimate : this.getPriceById("ENCHANTMENT_" + id);
    }

    /** Price of a rune, e.g. ("Lavatears", 1) for "Lavatears Rune I". Uses SkySens' own price data. */
    public @NotNull OptionalDouble getRunePrice(@NotNull String rune, int level) {
        return this.getPriceById(ItemIdUtil.toId(rune) + "_RUNE;" + level);
    }

    /** Loads SkySens' own price data once, for {@link #getPriceById(String)}. Lookups refresh it when stale. */
    public void prefetchOwn() {
        this.hypixel.prefetch();
    }

    /**
     * Loads price data once ahead of time, so the first lookup already has prices; lookups refresh it when
     * stale. Nothing is downloaded while SkyHanni is installed and working.
     */
    public void prefetch() {
        for (final ItemPriceProvider provider : this.providers) {
            if (provider.isAvailable()) {
                if (provider == this.hypixel) {
                    this.hypixel.prefetch();
                }
                return;
            }
        }
    }

}
