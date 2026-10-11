package de.vantrex.skysens.client.price;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.vantrex.skysens.client.util.ItemIdUtil;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The top of Hypixel's public bazaar order book: the highest buy order and the lowest sell offer of every
 * product, from a single request. Separate from the price providers, which only expose one price per item.
 */
public final class BazaarOrderBook {

    private static final String ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items";
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final Pattern ENCHANTMENT = Pattern.compile("^(.+) ([IVXLC]+)$");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    /** Item display name (normalized) to id, loaded once. */
    private static volatile Map<String, String> idByName = Map.of();

    /** Best prices of one product; 0 if there are no orders on that side. */
    public record Prices(double highestBuyOrder, double lowestSellOffer) {
    }

    private final Map<String, Prices> products;

    private BazaarOrderBook(Map<String, Prices> products) {
        this.products = products;
    }

    /** Blocking, call off the render thread. */
    public static BazaarOrderBook fetch() throws Exception {
        if (idByName.isEmpty()) {
            try {
                idByName = loadItemIds();
            } catch (Exception e) {
                // fall back to guessing ids from the names, the next fetch tries again
            }
        }
        final Map<String, Prices> products = new HashMap<>();
        for (final Map.Entry<String, JsonElement> entry : getJson(BAZAAR_URL).getAsJsonObject("products").entrySet()) {
            final JsonObject product = entry.getValue().getAsJsonObject();
            // buy_summary: buy orders, highest first; sell_summary: sell offers, lowest first
            products.put(entry.getKey(), new Prices(top(product, "buy_summary"), top(product, "sell_summary")));
        }
        return new BazaarOrderBook(products);
    }

    /** Best prices for an item by its display name, null if the product can't be found. */
    public @Nullable Prices find(@NotNull String itemName) {
        final String id = this.resolveId(itemName);
        return id == null ? null : this.products.get(id);
    }

    private @Nullable String resolveId(String itemName) {
        final String name = ChatFormatting.stripFormatting(itemName);
        final String clean = (name == null ? itemName : name).trim();
        final String known = idByName.get(clean.toLowerCase(Locale.ROOT));
        if (known != null && this.products.containsKey(known)) {
            return known;
        }
        final Matcher enchantment = ENCHANTMENT.matcher(clean);
        if (enchantment.matches()) {
            final String base = ItemIdUtil.toId(enchantment.group(1));
            final int level = ItemIdUtil.romanToInt(enchantment.group(2));
            for (final String id : new String[]{"ENCHANTMENT_" + base + "_" + level, "ENCHANTMENT_ULTIMATE_" + base + "_" + level}) {
                if (this.products.containsKey(id)) {
                    return id;
                }
            }
        }
        final String guess = ItemIdUtil.toId(clean);
        if (this.products.containsKey(guess)) {
            return guess;
        }
        // "Wither Essence" -> ESSENCE_WITHER
        if (clean.endsWith(" Essence")) {
            final String essence = "ESSENCE_" + ItemIdUtil.toId(clean.substring(0, clean.length() - " Essence".length()));
            if (this.products.containsKey(essence)) {
                return essence;
            }
        }
        return null;
    }

    private static double top(JsonObject product, String summary) {
        final JsonArray array = product.has(summary) && product.get(summary).isJsonArray() ? product.getAsJsonArray(summary) : null;
        if (array == null || array.isEmpty()) {
            return 0;
        }
        return array.get(0).getAsJsonObject().get("pricePerUnit").getAsDouble();
    }

    private static Map<String, String> loadItemIds() throws Exception {
        final Map<String, String> ids = new HashMap<>();
        for (final JsonElement element : getJson(ITEMS_URL).getAsJsonArray("items")) {
            final JsonObject item = element.getAsJsonObject();
            if (item.has("name") && item.has("id")) {
                ids.putIfAbsent(item.get("name").getAsString().trim().toLowerCase(Locale.ROOT), item.get("id").getAsString());
            }
        }
        return ids;
    }

    private static JsonObject getJson(String url) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "SkySens")
                .GET()
                .build();
        final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(url + " returned HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

}
