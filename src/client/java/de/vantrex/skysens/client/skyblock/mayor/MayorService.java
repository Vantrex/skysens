package de.vantrex.skysens.client.skyblock.mayor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reads the current mayor and minister perks from Hypixel's public election resource. */
public final class MayorService {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");
    private static final String ELECTION_URL = "https://api.hypixel.net/v2/resources/skyblock/election";
    private static final long REFRESH_INTERVAL_MS = 10 * 60 * 1000;
    private static final long RETRY_INTERVAL_MS = 60 * 1000;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Getter
    private static final MayorService instance = new MayorService();

    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile boolean slayerXpBuffActive;
    private volatile long nextRefreshAt;

    private MayorService() {
    }

    /** Starts a background refresh when the cached election data has expired. */
    public void refreshIfNeeded() {
        final long now = System.currentTimeMillis();
        if (now < this.nextRefreshAt || !this.refreshing.compareAndSet(false, true)) {
            return;
        }
        try {
            final HttpRequest request = HttpRequest.newBuilder(URI.create(ELECTION_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "SkySens")
                    .GET()
                    .build();
            HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete(this::completeRefresh);
        } catch (RuntimeException e) {
            this.completeRefresh(null, e);
        }
    }

    public boolean isSlayerXpBuffActive() {
        return this.slayerXpBuffActive;
    }

    private void completeRefresh(@Nullable HttpResponse<String> response, @Nullable Throwable error) {
        boolean success = false;
        boolean buffActive = false;
        try {
            if (error != null) {
                throw new IllegalStateException("Election request failed", error);
            }
            if (response == null || response.statusCode() != 200) {
                throw new IllegalStateException("Election request returned HTTP "
                        + (response == null ? "no response" : response.statusCode()));
            }
            final JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                throw new IllegalStateException("Election API reported an unsuccessful response");
            }
            buffActive = hasSlayerXpBuff(root);
            success = true;
        } catch (Exception e) {
            LOGGER.warn("Could not refresh Hypixel mayor data; assuming no Slayer XP Buff", e);
        } finally {
            this.slayerXpBuffActive = success && buffActive;
            this.nextRefreshAt = System.currentTimeMillis() + (success ? REFRESH_INTERVAL_MS : RETRY_INTERVAL_MS);
            this.refreshing.set(false);
        }
    }

    private static boolean hasSlayerXpBuff(JsonObject root) {
        final JsonObject mayor = object(root, "mayor");
        if (mayor != null && containsPerk(mayor.get("perks"))) {
            return true;
        }
        JsonObject minister = mayor == null ? null : object(mayor, "minister");
        if (minister == null) {
            minister = object(root, "minister");
        }
        if (minister != null && isSlayerXpBuff(minister.get("perk"))) {
            return true;
        }
        return mayor != null && electionMinisterHasSlayerXpBuff(mayor);
    }

    private static boolean electionMinisterHasSlayerXpBuff(JsonObject mayor) {
        final JsonObject election = object(mayor, "election");
        final JsonElement candidatesElement = election == null ? null : election.get("candidates");
        if (candidatesElement == null || !candidatesElement.isJsonArray()) {
            return false;
        }
        final JsonElement mayorNameElement = mayor.get("name");
        final String mayorName = mayorNameElement == null || mayorNameElement.isJsonNull()
                ? "" : mayorNameElement.getAsString();
        JsonObject ministerCandidate = null;
        long mostVotes = Long.MIN_VALUE;
        for (final JsonElement candidateElement : candidatesElement.getAsJsonArray()) {
            if (!candidateElement.isJsonObject()) {
                continue;
            }
            final JsonObject candidate = candidateElement.getAsJsonObject();
            final JsonElement name = candidate.get("name");
            final JsonElement votes = candidate.get("votes");
            if (name == null || name.isJsonNull() || votes == null || !votes.isJsonPrimitive()
                    || !votes.getAsJsonPrimitive().isNumber() || name.getAsString().equalsIgnoreCase(mayorName)) {
                continue;
            }
            final long candidateVotes = votes.getAsLong();
            if (candidateVotes > mostVotes) {
                ministerCandidate = candidate;
                mostVotes = candidateVotes;
            }
        }
        return ministerCandidate != null && containsMinisterPerk(ministerCandidate.get("perks"));
    }

    private static boolean containsMinisterPerk(@Nullable JsonElement perks) {
        if (perks == null || !perks.isJsonArray()) {
            return false;
        }
        for (final JsonElement perk : perks.getAsJsonArray()) {
            if (!perk.isJsonObject()) {
                continue;
            }
            final JsonObject perkObject = perk.getAsJsonObject();
            final JsonElement minister = perkObject.get("minister");
            if (minister != null && !minister.isJsonNull() && minister.getAsBoolean()
                    && isSlayerXpBuff(perk)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPerk(@Nullable JsonElement perks) {
        if (perks == null || !perks.isJsonArray()) {
            return false;
        }
        final JsonArray array = perks.getAsJsonArray();
        for (final JsonElement perk : array) {
            if (isSlayerXpBuff(perk)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSlayerXpBuff(@Nullable JsonElement perk) {
        if (perk == null || perk.isJsonNull()) {
            return false;
        }
        if (perk.isJsonObject()) {
            final JsonElement name = perk.getAsJsonObject().get("name");
            return name != null && !name.isJsonNull() && "Slayer XP Buff".equalsIgnoreCase(name.getAsString());
        }
        return perk.isJsonPrimitive() && "Slayer XP Buff".equalsIgnoreCase(perk.getAsString());
    }

    private static @Nullable JsonObject object(JsonObject parent, String key) {
        final JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

}