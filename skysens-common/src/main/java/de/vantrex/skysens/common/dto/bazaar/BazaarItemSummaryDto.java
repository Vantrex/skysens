package de.vantrex.skysens.common.dto.bazaar;

import java.time.Instant;

/**
 * Minimal bazaar aggregate (§5.5).
 *
 * <p><strong>Scope warning, recorded here on purpose.</strong> Hypixel already
 * publishes a public, unauthenticated {@code /skyblock/bazaar} endpoint with live
 * order books. Skysens must NOT re-serve that data — it would be a strictly worse,
 * staler copy and pointless load on both services. The only thing worth holding
 * here is what the public API does not provide: derived history and volatility
 * over a window. Until a feature actually needs that, this DTO stays unused.
 */
public record BazaarItemSummaryDto(
        String productId,
        double weeklyMeanBuyPrice,
        double weeklyMeanSellPrice,
        double volatility,
        Instant computedAt
) {
}
