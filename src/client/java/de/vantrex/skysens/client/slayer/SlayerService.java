package de.vantrex.skysens.client.slayer;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.slayer.SlayerCategory;
import de.vantrex.skysens.client.config.categories.slayer.SlayerFieldsAccordion;
import de.vantrex.skysens.client.profit.ProfitService;
import de.vantrex.skysens.client.profit.ProfitStats;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Session stats of slayer quests, read from chat: "SLAYER QUEST STARTED!", "SLAYER BOSS! The X spawned!",
 * "SLAYER QUEST COMPLETE!", "SLAYER QUEST FAILED!" and the "RNG Meter - N Stored XP" line of the completion box.
 * Lives in memory only, so it resets with the game. Called on the client thread.
 */
public final class SlayerService {

    private static final Pattern BOSS_SPAWN = Pattern.compile("^SLAYER BOSS! The (.+) spawned!$");
    private static final Pattern SLAYER_LEVEL = Pattern.compile("^(\\w+) Slayer LVL \\d+ - .*$");
    private static final Pattern RNG_METER = Pattern.compile("^RNG Meter - ([\\d,]+) Stored XP$");
    /** The boss names as shown on the spawn message, mapped to the slayer's name used in the level lines. */
    private static final Map<String, String> BOSS_TO_SLAYER = Map.of(
            "Revenant Horror", "Zombie",
            "Tarantula Broodfather", "Spider",
            "Sven Packmaster", "Wolf",
            "Voidgloom Seraph", "Enderman",
            "Inferno Demonlord", "Blaze",
            "Riftstalker Bloodfiend", "Vampire");

    @Getter
    private static final SlayerService instance = new SlayerService();

    /** Per boss (with tier, e.g. "Inferno Demonlord IV") in order of first appearance. */
    private final Map<String, BossStats> bosses = new LinkedHashMap<>();
    private final Map<String, Long> rngMeter = new LinkedHashMap<>();
    private @Nullable String currentBoss;
    private @Nullable String lastSlayerType;
    private long questStartMs;
    private long bossSpawnMs;
    private int totalKills;

    /** Stats of one boss type. Quest times only count quests whose start was seen. */
    public static final class BossStats {
        public int kills;
        public int failed;
        public long killTimeMs;
        public int killTimed;
        public long questTimeMs;
        public int questTimed;

        /** Kills per hour of questing, or NaN while there is too little data. */
        public double perHour() {
            return this.questTimed == 0 || this.questTimeMs < 60_000 ? Double.NaN : this.questTimed * 3_600_000.0 / this.questTimeMs;
        }
    }

    private SlayerService() {
    }

    /** Handles a chat line (formatting already removed). */
    public void onChatLine(@NotNull String line) {
        if (!config().enabled) {
            return;
        }
        final String text = line.trim();
        if (text.equals("SLAYER QUEST STARTED!")) {
            this.questStartMs = System.currentTimeMillis();
            this.bossSpawnMs = 0;
            return;
        }
        final Matcher spawn = BOSS_SPAWN.matcher(text);
        if (spawn.matches()) {
            this.currentBoss = spawn.group(1);
            this.bossSpawnMs = System.currentTimeMillis();
            return;
        }
        if (text.equals("SLAYER QUEST COMPLETE!")) {
            this.onComplete();
            return;
        }
        if (text.equals("SLAYER QUEST FAILED!")) {
            this.stats(this.currentBoss).failed++;
            this.bossSpawnMs = 0;
            this.questStartMs = 0;
            return;
        }
        final Matcher level = SLAYER_LEVEL.matcher(text);
        if (level.matches()) {
            this.lastSlayerType = level.group(1);
            return;
        }
        final Matcher rng = RNG_METER.matcher(text);
        if (rng.matches() && this.lastSlayerType != null) {
            this.rngMeter.put(this.lastSlayerType, Long.parseLong(rng.group(1).replace(",", "")));
        }
    }

    private void onComplete() {
        final long now = System.currentTimeMillis();
        final BossStats stats = this.stats(this.currentBoss);
        stats.kills++;
        this.totalKills++;
        if (this.bossSpawnMs > 0) {
            stats.killTimeMs += now - this.bossSpawnMs;
            stats.killTimed++;
        }
        if (this.questStartMs > 0) {
            stats.questTimeMs += now - this.questStartMs;
            stats.questTimed++;
        }
        this.bossSpawnMs = 0;
        // the next quest starts right away, its start message sets this again
        this.questStartMs = 0;
        final int every = (int) config().summaryEveryBosses;
        if (every > 0 && this.totalKills % every == 0) {
            ClientUtil.sendPrefixed("Slayer: " + this.summary());
        }
    }

    private BossStats stats(@Nullable String boss) {
        return this.bosses.computeIfAbsent(boss == null ? "Unknown boss" : boss, name -> new BossStats());
    }

    /** A short summary of this session, at most three boss types, for {@code !slayer} and the chat summary. */
    public @NotNull String summary() {
        if (this.bosses.isEmpty()) {
            return "No slayer bosses this session";
        }
        final SlayerFieldsAccordion fields = config().fields;
        final List<Map.Entry<String, BossStats>> entries = new ArrayList<>(this.bosses.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, BossStats> e) -> e.getValue().kills + e.getValue().failed).reversed());
        final List<String> parts = new ArrayList<>();
        for (final Map.Entry<String, BossStats> entry : entries.subList(0, Math.min(3, entries.size()))) {
            parts.add(describe(entry.getKey(), entry.getValue(), fields));
        }
        if (fields.profit) {
            final String profit = this.profit();
            if (profit != null) {
                parts.add(profit);
            }
        }
        return String.join(" | ", parts);
    }

    private String describe(String boss, BossStats stats, SlayerFieldsAccordion fields) {
        final StringBuilder out = new StringBuilder(boss).append(": ").append(stats.kills).append(stats.kills == 1 ? " boss" : " bosses");
        final double perHour = stats.perHour();
        if (fields.bossesPerHour && !Double.isNaN(perHour)) {
            out.append(String.format(Locale.ROOT, " (%.1f/h)", perHour));
        }
        if (fields.averageKillTime && stats.killTimed > 0) {
            out.append(" | avg kill ").append(formatDuration(stats.killTimeMs / stats.killTimed));
        }
        if (fields.averageQuestTime && stats.questTimed > 0) {
            out.append(" | avg quest ").append(formatDuration(stats.questTimeMs / stats.questTimed));
        }
        if (fields.failed && stats.failed > 0) {
            out.append(" | ").append(stats.failed).append(" failed");
        }
        if (fields.rngMeter) {
            final String slayer = slayerOf(boss);
            final Long xp = slayer == null ? null : this.rngMeter.get(slayer);
            if (xp != null) {
                out.append(" | RNG ").append(NumberUtil.formatCoins(xp)).append(" XP");
            }
        }
        return out.toString();
    }

    /**
     * Profit per boss and per hour of the profit tracker's slayer activity, or null if it has none. The tracker
     * doesn't split by boss type, so this covers all slayers of the session.
     */
    private @Nullable String profit() {
        if (this.totalKills == 0) {
            return null;
        }
        final ProfitService.ActivityProfit activity = ProfitService.getInstance().getCurrentActivity(false);
        if (activity == null || !activity.name().toLowerCase(Locale.ROOT).contains("slayer")) {
            return null;
        }
        final ProfitStats stats = activity.stats();
        final StringBuilder out = new StringBuilder(formatSigned(stats.profit / this.totalKills)).append("/boss");
        final double perHour = stats.perHour();
        if (!Double.isNaN(perHour)) {
            out.append(" (").append(formatSigned(perHour)).append("/h)");
        }
        return out.toString();
    }

    private static @Nullable String slayerOf(String boss) {
        for (final Map.Entry<String, String> entry : BOSS_TO_SLAYER.entrySet()) {
            if (boss.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String formatSigned(double coins) {
        return (coins < 0 ? "-" : "") + NumberUtil.formatCoins(Math.abs(coins));
    }

    private static String formatDuration(long ms) {
        final long seconds = Math.round(ms / 1000.0);
        return seconds >= 60 ? seconds / 60 + "m " + seconds % 60 + "s" : seconds + "s";
    }

    private static SlayerCategory config() {
        return SkysensConfig.getInstance().slayerCategory;
    }

}
