package de.vantrex.skysens.client.price;

import de.vantrex.skysens.client.util.ClientUtil;
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

    private final List<ItemPriceProvider> providers = List.of(
            new SkyHanniPriceProvider(),
            new HypixelPriceProvider()
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
     * Loads price data ahead of time, so the first lookup already has prices. Only the providers
     * that would be asked are refreshed: nothing is downloaded while SkyHanni is installed and working.
     */
    public void prefetch() {
        for (final ItemPriceProvider provider : this.providers) {
            if (provider.isAvailable()) {
                provider.refresh();
                if (!(provider instanceof HypixelPriceProvider)) {
                    return;
                }
            }
        }
    }

}
