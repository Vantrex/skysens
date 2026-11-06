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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private volatile Election election = Election.EMPTY;
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

    /**
     * The current mayor and minister with their perks (original spelling), as a snapshot.
     *
     * @param mayor        the mayor's name, null if the API didn't give one
     * @param minister     the minister's name, null if none
     * @param mayorPerks   the mayor's perks
     * @param ministerPerks the minister's perks (usually one)
     */
    public record Election(@Nullable String mayor, @Nullable String minister, List<String> mayorPerks, List<String> ministerPerks) {
        static final Election EMPTY = new Election(null, null, List.of(), List.of());
    }

    /** The mayor and minister of the last successful fetch; empty values while {@link #isDataKnown()} is false. */
    public Election getElection() {
        return this.election;
    }

    public @Nullable String getMayorName() {
        return this.election.mayor();
    }

    public @Nullable String getMinisterName() {
        return this.election.minister();
    }

    private static Election collectElection(JsonObject root) {
        final JsonObject mayor = object(root, "mayor");
        if (mayor == null) {
            return Election.EMPTY;
        }
        final List<String> mayorPerks = new ArrayList<>();
        final JsonElement perks = mayor.get("perks");
        if (perks != null && perks.isJsonArray()) {
            for (final JsonElement perk : perks.getAsJsonArray()) {
                addOriginal(mayorPerks, perk);
            }
        }
        JsonObject minister = object(mayor, "minister");
        if (minister == null) {
            minister = object(root, "minister");
        }
        final List<String> ministerPerks = new ArrayList<>();
        String ministerName = null;
        if (minister != null) {
            ministerName = text(minister.get("name"));
            addOriginal(ministerPerks, minister.get("perk"));
        } else {
            final JsonObject candidate = electionMinister(mayor);
            if (candidate != null) {
                ministerName = text(candidate.get("name"));
                final JsonElement candidatePerks = candidate.get("perks");
                if (candidatePerks != null && candidatePerks.isJsonArray()) {
                    for (final JsonElement perk : candidatePerks.getAsJsonArray()) {
                        if (perk.isJsonObject() && perk.getAsJsonObject().has("minister")
                                && !perk.getAsJsonObject().get("minister").isJsonNull() && perk.getAsJsonObject().get("minister").getAsBoolean()) {
                            addOriginal(ministerPerks, perk);
                        }
                    }
                }
            }
        }
        return new Election(text(mayor.get("name")), ministerName, List.copyOf(mayorPerks), List.copyOf(ministerPerks));
    }

    private static @Nullable String text(@Nullable JsonElement element) {
        return element == null || element.isJsonNull() || !element.isJsonPrimitive() ? null : element.getAsString();
    }

    private static void addOriginal(List<String> out, @Nullable JsonElement perk) {
        if (perk == null || perk.isJsonNull()) {
            return;
        }
        final String name = perk.isJsonObject() ? text(perk.getAsJsonObject().get("name")) : text(perk);
        if (name != null && !name.isBlank()) {
            out.add(name);
        }
    }

    private void completeRefresh(@Nullable HttpResponse<String> response, @Nullable Throwable error) {
        boolean success = false;
        Election fresh = Election.EMPTY;
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
            fresh = collectElection(root);
            success = true;
        } catch (Exception e) {
            LOGGER.warn("Could not refresh Hypixel mayor data; keeping the previous perks", e);
        } finally {
            if (success) {
                this.activePerks = perks;
                this.election = fresh;
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
