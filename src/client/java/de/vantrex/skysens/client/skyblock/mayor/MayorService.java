package de.vantrex.skysens.client.skyblock.mayor;

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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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
    /** Lower-cased names of the perks of the mayor and the minister. */
    private volatile Set<String> activePerks = Set.of();
    private volatile boolean dataKnown;
    /** Counts successful refreshes, so a consumer can tell that the data is newer than something it saw. */
    private volatile long dataVersion;
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
        return this.hasPerk("Slayer XP Buff");
    }

    /** Whether the mayor or the minister has this perk (case-insensitive); false while the data isn't known. */
    public boolean hasPerk(String perkName) {
        return this.activePerks.contains(perkName.toLowerCase(Locale.ROOT));
    }

    /** Whether the election data was fetched successfully at least once; before that {@link #hasPerk} says false. */
    public boolean isDataKnown() {
        return this.dataKnown;
    }

    /** Increases with every successful refresh. */
    public long getDataVersion() {
        return this.dataVersion;
    }

    /**
     * Whether Diana's "Mythological Ritual" perk is active. Jerry's "Perkpocalypse" is deliberately not counted: the
     * election API doesn't say which mayor's perks it currently borrows, so it can't be told whether it is Diana's.
     */
    public boolean isMythologicalRitualActive() {
        return this.hasPerk("Mythological Ritual");
    }

    private void completeRefresh(@Nullable HttpResponse<String> response, @Nullable Throwable error) {
        boolean success = false;
        Set<String> perks = Set.of();
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
            perks = collectPerks(root);
            success = true;
        } catch (Exception e) {
            LOGGER.warn("Could not refresh Hypixel mayor data; keeping the previous perks", e);
        } finally {
            if (success) {
                this.activePerks = perks;
                this.dataKnown = true;
                this.dataVersion++;
            }
            this.nextRefreshAt = System.currentTimeMillis() + (success ? REFRESH_INTERVAL_MS : RETRY_INTERVAL_MS);
            this.refreshing.set(false);
        }
    }

    /** The lower-cased names of all perks of the mayor and the minister. */
    private static Set<String> collectPerks(JsonObject root) {
        final Set<String> perks = new HashSet<>();
        final JsonObject mayor = object(root, "mayor");
        if (mayor != null) {
            addPerks(perks, mayor.get("perks"), false);
        }
        JsonObject minister = mayor == null ? null : object(mayor, "minister");
        if (minister == null) {
            minister = object(root, "minister");
        }
        if (minister != null) {
            addName(perks, minister.get("perk"));
        }
        if (mayor != null) {
            final JsonObject ministerCandidate = electionMinister(mayor);
            if (ministerCandidate != null) {
                addPerks(perks, ministerCandidate.get("perks"), true);
            }
        }
        return Set.copyOf(perks);
    }

    /** The runner-up of the election, who becomes minister, or null if the candidates aren't there. */
    private static @Nullable JsonObject electionMinister(JsonObject mayor) {
        final JsonObject election = object(mayor, "election");
        final JsonElement candidatesElement = election == null ? null : election.get("candidates");
        if (candidatesElement == null || !candidatesElement.isJsonArray()) {
            return null;
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
        return ministerCandidate;
    }

    /** Adds the perks of an array; with {@code ministerOnly} only those flagged {@code "minister": true}. */
    private static void addPerks(Set<String> out, @Nullable JsonElement perks, boolean ministerOnly) {
        if (perks == null || !perks.isJsonArray()) {
            return;
        }
        for (final JsonElement perk : perks.getAsJsonArray()) {
            if (ministerOnly) {
                if (!perk.isJsonObject()) {
                    continue;
                }
                final JsonElement minister = perk.getAsJsonObject().get("minister");
                if (minister == null || minister.isJsonNull() || !minister.getAsBoolean()) {
                    continue;
                }
            }
            addName(out, perk);
        }
    }

    private static void addName(Set<String> out, @Nullable JsonElement perk) {
        if (perk == null || perk.isJsonNull()) {
            return;
        }
        if (perk.isJsonObject()) {
            final JsonElement name = perk.getAsJsonObject().get("name");
            if (name != null && !name.isJsonNull()) {
                out.add(name.getAsString().toLowerCase(Locale.ROOT));
            }
        } else if (perk.isJsonPrimitive()) {
            out.add(perk.getAsString().toLowerCase(Locale.ROOT));
        }
    }

    private static @Nullable JsonObject object(JsonObject parent, String key) {
        final JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

}
