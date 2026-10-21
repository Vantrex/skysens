package de.vantrex.skysens.client.price;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.vantrex.skysens.client.SkysensClient;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SkySens' own price data, used when SkyHanni is not installed:
 * <ul>
 *     <li>item names to ids: Hypixel's item list (loaded once)</li>
 *     <li>bazaar instant sell prices: Hypixel's bazaar API</li>
 *     <li>lowest BIN prices: EliteSkyblock's lowest BIN API (also used by SkyHanni), keyed by NEU item id,
 *     pets as {@code ENDER_DRAGON;4}</li>
 * </ul>
 * Prices are refreshed every {@value #PRICE_MAX_AGE_MS} ms while something asks for them.
 */
final class HypixelPriceProvider implements ItemPriceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");
    private static final String ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items";
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String LOWEST_BIN_URL = "https://api.eliteskyblock.com/resources/auctions/neu?mode=smooth";
    private static final long PRICE_MAX_AGE_MS = 5 * 60 * 1000;
    private static final long RETRY_AFTER_ERROR_MS = 60 * 1000;
    private static final List<String> PET_RARITIES = List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Map<String, String> idByName = Map.of();
    private volatile Map<String, Double> bazaarPrices = Map.of();
    private volatile Map<String, Double> lowestBins = Map.of();
    private volatile long nextRefreshAt;

    @Override
    public @NotNull String name() {
        return "SkySens";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /** Loads the data once, so the first lookup already has prices. Later refreshes happen on lookup. */
    void prefetch() {
        if (this.nextRefreshAt == 0) {
            this.refresh();
        }
    }

    @Override
    public void refresh() {
        if (System.currentTimeMillis() < this.nextRefreshAt || !this.refreshing.compareAndSet(false, true)) {
            return;
        }
        SkysensClient.SCHEDULER.execute(() -> {
            boolean failed = false;
            try {
                if (this.idByName.isEmpty()) {
                    this.idByName = this.loadItemIds();
                }
                this.bazaarPrices = this.loadBazaar();
                this.lowestBins = this.loadLowestBins();
            } catch (Exception e) {
                failed = true;
                LOGGER.warn("Could not load item prices", e);
            } finally {
                this.nextRefreshAt = System.currentTimeMillis() + (failed ? RETRY_AFTER_ERROR_MS : PRICE_MAX_AGE_MS);
                this.refreshing.set(false);
            }
        });
    }

    @Override
    public @NotNull OptionalDouble price(@NotNull String itemName) {
        this.refresh();
        final String id = this.toId(itemName);
        return id == null ? OptionalDouble.empty() : this.lookUp(id);
    }

    /** Price by item id: bazaar ids ("ENCHANTMENT_ULTIMATE_WISDOM_2", "ESSENCE_WITHER") or NEU lowest BIN ids. */
    @NotNull OptionalDouble priceById(@NotNull String id) {
        this.refresh();
        return this.lookUp(id);
    }

    private OptionalDouble lookUp(String id) {
        final Double bazaar = this.bazaarPrices.get(id);
        if (bazaar != null && bazaar > 0) {
            return OptionalDouble.of(bazaar);
        }
        final Double bin = this.lowestBins.get(id);
        if (bin != null && bin > 0) {
            return OptionalDouble.of(bin);
        }
        // a bazaar item without buy orders (e.g. Scorched Books) can't be insta-sold, it's worth nothing right now
        return bazaar != null ? OptionalDouble.of(0) : OptionalDouble.empty();
    }

    private @Nullable String toId(String itemName) {
        final String id = this.idByName.get(normalize(itemName));
        if (id != null) {
            return id;
        }
        // pets aren't in Hypixel's item list: "Legendary Ender Dragon" -> "ENDER_DRAGON;4"
        final String[] parts = itemName.trim().split(" ", 2);
        if (parts.length == 2) {
            final int rarity = PET_RARITIES.indexOf(parts[0].toUpperCase(Locale.ROOT));
            if (rarity >= 0) {
                final String petId = parts[1].replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(Locale.ROOT) + ";" + rarity;
                if (this.lowestBins.containsKey(petId)) {
                    return petId;
                }
            }
        }
        return null;
    }

    private static String normalize(String name) {
        final String stripped = ChatFormatting.stripFormatting(name);
        return (stripped == null ? name : stripped).trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, String> loadItemIds() throws Exception {
        final Map<String, String> ids = new HashMap<>();
        for (final JsonElement element : this.getJson(ITEMS_URL).getAsJsonArray("items")) {
            final JsonObject item = element.getAsJsonObject();
            if (item.has("name") && item.has("id")) {
                // first one wins, so a rare duplicate name doesn't replace the common item
                ids.putIfAbsent(normalize(item.get("name").getAsString()), item.get("id").getAsString());
            }
        }
        return ids;
    }

    private Map<String, Double> loadBazaar() throws Exception {
        final Map<String, Double> prices = new HashMap<>();
        for (final Map.Entry<String, JsonElement> product : this.getJson(BAZAAR_URL).getAsJsonObject("products").entrySet()) {
            final JsonObject status = product.getValue().getAsJsonObject().getAsJsonObject("quick_status");
            if (status != null && status.has("sellPrice")) {
                prices.put(product.getKey(), status.get("sellPrice").getAsDouble());
            }
        }
        return prices;
    }

    private Map<String, Double> loadLowestBins() throws Exception {
        final Map<String, Double> prices = new HashMap<>();
        for (final Map.Entry<String, JsonElement> entry : this.getJson(LOWEST_BIN_URL).entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                prices.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }
        return prices;
    }

    private JsonObject getJson(String url) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "SkySens")
                .GET()
                .build();
        final HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(url + " returned HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

}
