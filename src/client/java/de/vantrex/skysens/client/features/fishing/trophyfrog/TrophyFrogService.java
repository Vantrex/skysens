package de.vantrex.skysens.client.features.fishing.trophyfrog;

import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import de.vantrex.skysens.client.skyblock.player.ProfileService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the Trophy Frog counts of the current account and profile. There is no API for them, so they come from two places:
 * <ul>
 *     <li>the Trophy Frogs menu, whose frogs list each tier in their lore ("Gold ✔ (16)", "Diamond ✖"). It is exact
 *     and overwrites what was counted before;</li>
 *     <li>the catch message ("♔ TROPHY FROG! You caught a Common Frog BRONZE!"), which adds one (or "x2") to it.</li>
 * </ul>
 * Client thread.
 */
public final class TrophyFrogService {

    public static final String MENU_TITLE = "Trophy Frogs";

    /**
     * The prefix symbol differs between versions (♔ or a resource pack glyph), so the match starts after it. Repeated
     * messages can end with a stack counter ("BRONZE! (2)"), that is still one catch.
     */
    private static final Pattern CATCH_PATTERN = Pattern.compile(
            "TROPHY FROG! You caught(?: an?)? (?<name>.+?) (?<tier>BRONZE|SILVER|GOLD|DIAMOND)(?: x(?<amount>[\\d,]+))?!(?: \\(\\d[\\d,]*\\))?$");
    /** A tier line in a frog's lore: "Gold ✔ (16)" when caught, "Diamond ✖" when not. */
    private static final Pattern TIER_LINE_PATTERN = Pattern.compile("^(?<tier>Bronze|Silver|Gold|Diamond) \\S+(?: \\((?<amount>[\\d,]+)\\))?$");
    /** How long a caught tier is highlighted in the HUD. */
    private static final long HIGHLIGHT_MS = 5_000;
    /**
     * The catch rate for the pity estimates is measured over at most this much time on the Lotus Atoll: all of it
     * this session until then, the most recent hour after.
     */
    public static final long RATE_WINDOW_MS = 60 * 60_000;
    /** Below this much measured time the catch rate is too uncertain to estimate with. */
    private static final long MIN_RATE_WINDOW_MS = 60_000;
    /**
     * A longer gap between two {@link #onLotusAtoll} calls means the player was elsewhere (or the game paused), that
     * time isn't counted.
     */
    private static final long MAX_TICK_GAP_MS = 1_000;

    @Getter
    private static final TrophyFrogService instance = new TrophyFrogService();

    /** The frog name and tier caught last, highlighted until {@link #highlightUntilMs}. */
    private @Nullable String highlightedFrog;
    private @Nullable TrophyFrogTier highlightedTier;
    private long highlightUntilMs;

    /**
     * The catches of the last {@link #RATE_WINDOW_MS} on the Lotus Atoll, oldest first, stamped with
     * {@link #atollTimeMs}. Only kept in memory: the rate describes the current fishing session. Belongs to
     * {@link #rateProfile}, cleared when the profile changes.
     */
    private final Deque<RecentCatch> recentCatches = new ArrayDeque<>();
    private @Nullable TrophyFrogRepository.Profile rateProfile;
    /** Time spent on the Lotus Atoll with {@link #rateProfile} this game session, a clock that stops elsewhere. */
    private long atollTimeMs;
    /** When {@link #onLotusAtoll} was last called (wall clock), 0 before the first call. */
    private long lastAtollTickMs;

    private TrophyFrogService() {
    }

    /** One frog item in the open menu: its stripped name and lore. */
    public record MenuItem(@NotNull String name, @NotNull List<String> lore) {
    }

    /** A parsed catch message. */
    record Catch(@NotNull String frog, @NotNull TrophyFrogTier tier, int amount) {
    }

    private record RecentCatch(@NotNull String frog, int amount, long atMs) {
    }

    /** How many catches of one frog there were in the measured time, at most {@link #RATE_WINDOW_MS}. */
    record CatchRate(int catches, long windowMs) {
        /** The estimated time for the given amount of catches, null if there were no catches to estimate with. */
        @Nullable Long timeFor(int catches) {
            return this.catches == 0 ? null : (long) ((double) catches * this.windowMs / this.catches);
        }
    }

    /**
     * The counts of the current profile of the logged in account (by UUID, profiles of different accounts can share a
     * name), by profile id or by name while the id is unknown. Null while the profile is unknown, then nothing is
     * counted.
     */
    public @Nullable TrophyFrogRepository.Profile current() {
        final ProfileService profiles = ProfileService.getInstance();
        final String name = profiles.getCurrentProfile();
        if (name == null) {
            return null;
        }
        return repository().account(Minecraft.getInstance().getUser().getProfileId().toString())
                .profile(profiles.getCurrentProfileId(), name);
    }

    /** Takes the counts from the open Trophy Frogs menu. Items without tier lines (buttons, panes) are skipped. */
    public void onMenu(@NotNull List<MenuItem> items) {
        final TrophyFrogRepository.Profile profile = this.current();
        if (profile == null) {
            return;
        }
        boolean changed = false;
        // rebuilt in menu order, frogs only seen in chat so far go to the end
        final Map<String, Map<TrophyFrogTier, Integer>> ordered = new LinkedHashMap<>();
        for (final MenuItem item : items) {
            final Map<TrophyFrogTier, Integer> counts = parseTiers(item.lore());
            if (counts.isEmpty()) {
                continue;
            }
            final Map<TrophyFrogTier, Integer> stored = profile.frog(item.name());
            if (!stored.equals(counts)) {
                stored.clear();
                stored.putAll(counts);
                changed = true;
            }
            ordered.put(item.name(), stored);
        }
        final boolean foundFrog = !ordered.isEmpty();
        if (foundFrog) {
            profile.frogs.forEach(ordered::putIfAbsent);
            if (!List.copyOf(ordered.keySet()).equals(List.copyOf(profile.frogs.keySet()))) {
                profile.frogs = ordered;
                changed = true;
            }
        }
        // the lore can arrive a tick after the items, only count the menu as read once a frog was found in it
        if (foundFrog && !profile.menuRead) {
            profile.menuRead = true;
            changed = true;
            ClientUtil.sendDebug("Trophy Frogs: read the menu, " + profile.frogs.size() + " frogs");
        }
        if (changed) {
            save();
        }
    }

    /** Counts a catch message, ignores any other line. Takes a stripped, normalized chat line. */
    public void onChatLine(@NotNull String line) {
        final Catch caught = parseCatch(line);
        if (caught == null) {
            if (line.contains("TROPHY FROG!")) {
                ClientUtil.sendDebug("Trophy Frogs: unknown catch message: " + line);
            }
            return;
        }
        final TrophyFrogRepository.Profile profile = this.current();
        if (profile == null) {
            ClientUtil.sendDebug("Trophy Frogs: profile unknown, catch not counted: " + line);
            return;
        }
        profile.frog(caught.frog()).merge(caught.tier(), caught.amount(), Integer::sum);
        this.recordCatch(profile, caught);
        this.highlightedFrog = caught.frog();
        this.highlightedTier = caught.tier();
        this.highlightUntilMs = System.currentTimeMillis() + HIGHLIGHT_MS;
        save();
    }

    /**
     * Advances the Lotus Atoll clock of the catch rate, called every tick while the player is on the Lotus Atoll.
     * Time away from it (and game pauses) doesn't count, see {@link #MAX_TICK_GAP_MS}.
     */
    public void onLotusAtoll() {
        final TrophyFrogRepository.Profile profile = this.current();
        if (profile == null) {
            return;
        }
        this.switchProfile(profile);
        final long now = System.currentTimeMillis();
        final long gap = now - this.lastAtollTickMs;
        if (this.lastAtollTickMs != 0 && gap > 0 && gap <= MAX_TICK_GAP_MS) {
            this.atollTimeMs += gap;
        }
        this.lastAtollTickMs = now;
        while (!this.recentCatches.isEmpty()
                && this.recentCatches.peekFirst().atMs() < this.atollTimeMs - RATE_WINDOW_MS) {
            this.recentCatches.removeFirst();
        }
    }

    private void recordCatch(TrophyFrogRepository.Profile profile, Catch caught) {
        this.switchProfile(profile);
        this.recentCatches.addLast(new RecentCatch(caught.frog(), caught.amount(), this.atollTimeMs));
    }

    /** Starts the measurement over when the profile changed, the rate of another profile doesn't apply. */
    private void switchProfile(TrophyFrogRepository.Profile profile) {
        if (profile != this.rateProfile) {
            this.rateProfile = profile;
            this.recentCatches.clear();
            this.atollTimeMs = 0;
        }
    }

    /**
     * The catches of the frog in the last {@link #RATE_WINDOW_MS} spent on the Lotus Atoll this game session, over
     * less time if less was spent there. Null while less than a minute was measured on the current profile.
     */
    public @Nullable CatchRate catchRate(@NotNull String frog) {
        if (this.rateProfile == null || this.rateProfile != this.current()) {
            return null;
        }
        final long windowMs = Math.min(RATE_WINDOW_MS, this.atollTimeMs);
        if (windowMs < MIN_RATE_WINDOW_MS) {
            return null;
        }
        int catches = 0;
        for (final RecentCatch recent : this.recentCatches) {
            if (recent.atMs() >= this.atollTimeMs - windowMs && recent.frog().equals(frog)) {
                catches += recent.amount();
            }
        }
        return new CatchRate(catches, windowMs);
    }

    /** The tier of the frog caught in the last few seconds, null if the given frog wasn't. */
    public @Nullable TrophyFrogTier highlightedTier(@NotNull String frog) {
        return System.currentTimeMillis() < this.highlightUntilMs && Objects.equals(this.highlightedFrog, frog)
                ? this.highlightedTier : null;
    }

    static @Nullable Catch parseCatch(@NotNull String line) {
        final Matcher matcher = CATCH_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        final TrophyFrogTier tier = TrophyFrogTier.byName(matcher.group("tier"));
        if (tier == null) {
            return null;
        }
        final String amount = matcher.group("amount");
        return new Catch(matcher.group("name"), tier, amount == null ? 1 : (int) NumberUtil.parseNumber(amount));
    }

    /** The amount per tier from a frog's lore, empty if the lore has no tier lines. A tier without an amount is 0. */
    static @NotNull Map<TrophyFrogTier, Integer> parseTiers(@NotNull List<String> lore) {
        final Map<TrophyFrogTier, Integer> counts = new EnumMap<>(TrophyFrogTier.class);
        for (final String line : lore) {
            final Matcher matcher = TIER_LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            final TrophyFrogTier tier = TrophyFrogTier.byName(matcher.group("tier"));
            final String amount = matcher.group("amount");
            if (tier != null) {
                counts.put(tier, amount == null ? 0 : (int) NumberUtil.parseNumber(amount));
            }
        }
        return counts;
    }

    /** Total catches over all tiers of one frog. */
    static int total(@NotNull Map<TrophyFrogTier, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static TrophyFrogRepository repository() {
        return RepositoryRegistry.TROPHY_FROG_REPOSITORY;
    }

    private static void save() {
        RepositoryRegistry.saveRepository(repository());
    }

}
