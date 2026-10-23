package de.vantrex.skysens.client.features.slayer;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommandService;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.slayer.config.SlayerCategory;
import de.vantrex.skysens.client.features.slayer.config.SlayerFieldsAccordion;
import de.vantrex.skysens.client.skyblock.event.SlayerBossCocoonEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerBossSpawnEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestEndEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestStartEvent;
import de.vantrex.skysens.client.skyblock.profit.ProfitService;
import de.vantrex.skysens.client.skyblock.profit.ProfitStats;
import de.vantrex.skysens.client.skyblock.slayer.SlayerTracker;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import de.vantrex.skysens.client.util.TimeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Session stats of slayer quests. Quest start, boss spawn, cocoon and quest end come from {@link SlayerTracker}'s events,
 * the "RNG Meter - N Stored XP" line of the completion box from chat.
 * Lives in memory only, so it resets with the game. Called on the client thread.
 */
public final class SlayerService {

    private static final Pattern SLAYER_LEVEL = Pattern.compile("^(\\w+) Slayer LVL \\d+ - .*$");
    private static final Pattern RNG_METER = Pattern.compile("^RNG Meter - ([\\d,]+) Stored XP$");
    /** The sidebar line with the quest progress before the boss spawns, e.g. "(1,200/2,400) Combat XP". */
    private static final Pattern QUEST_PROGRESS = Pattern.compile("^\\([\\d,.kKmM]+/[\\d,.kKmM]+\\) .+$");
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
    /** Time of the running quest and boss fight that counts, i.e. without idle stretches. */
    private long questActiveMs;
    private long killActiveMs;
    /** The boss was cocooned: the kill time stops, the quest time runs until the cocoon hatches. */
    private boolean bossCocooned;
    /** Questing time of the session for profit per hour, with its own (longer) idle pause. */
    private long profitActiveMs;
    private long lastTickMs;
    private long lastInputMs;
    /** The quest progress line of the sidebar (e.g. "(1,200/2,400) Combat XP") and when it last changed. */
    private @Nullable String progressLine;
    private long progressChangedMs;

    /** Stats of one boss type, times without idle stretches. Quest times only count quests whose start was seen. */
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
        final String text = ChatUtil.normalize(line);
        if (!this.handleSlayerLevel(text)) {
            this.handleRngMeter(text);
        }
    }

    /** A quest whose start wasn't seen (noticed later by the tracker) isn't timed. */
    public void onQuestStart(@NotNull SlayerQuestStartEvent event) {
        if (!config().enabled) {
            return;
        }
        final long now = System.currentTimeMillis();
        this.questStartMs = event.isStartSeen() ? now : 0;
        this.bossSpawnMs = 0;
        this.bossCocooned = false;
        this.questActiveMs = 0;
        this.killActiveMs = 0;
        this.progressLine = null;
        this.progressChangedMs = now;
    }

    public void onBossSpawn(@NotNull SlayerBossSpawnEvent event) {
        if (!config().enabled) {
            return;
        }
        if (event.getBoss() != null) {
            this.currentBoss = event.getBoss();
        }
        this.bossSpawnMs = System.currentTimeMillis();
        this.bossCocooned = false;
        this.killActiveMs = 0;
    }

    public void onBossCocoon(@NotNull SlayerBossCocoonEvent event) {
        if (!config().enabled) {
            return;
        }
        this.bossCocooned = true;
    }

    public void onQuestEnd(@NotNull SlayerQuestEndEvent event) {
        if (!config().enabled) {
            return;
        }
        switch (event.getReason()) {
            case COMPLETED -> this.onComplete();
            case FAILED -> {
                this.stats(this.currentBoss).failed++;
                this.endQuest();
            }
            case CANCELLED -> this.endQuest();
        }
    }

    /** The level line comes right before the RNG meter line and tells which slayer the meter belongs to. */
    private boolean handleSlayerLevel(String text) {
        final Matcher level = SLAYER_LEVEL.matcher(text);
        if (!level.matches()) {
            return false;
        }
        this.lastSlayerType = level.group(1);
        return true;
    }

    private void handleRngMeter(String text) {
        final Matcher rng = RNG_METER.matcher(text);
        if (rng.matches() && this.lastSlayerType != null) {
            this.rngMeter.put(this.lastSlayerType, Long.parseLong(rng.group(1).replace(",", "")));
        }
    }

    private void onComplete() {
        final BossStats stats = this.stats(this.currentBoss);
        stats.kills++;
        this.totalKills++;
        if (this.bossSpawnMs > 0) {
            stats.killTimeMs += this.killActiveMs;
            stats.killTimed++;
        }
        if (this.questStartMs > 0) {
            stats.questTimeMs += this.questActiveMs;
            stats.questTimed++;
        }
        // the next quest starts right away, its start message sets the timers again
        this.endQuest();
        final int every = (int) config().summaryEveryBosses;
        if (every > 0 && this.totalKills % every == 0) {
            final String summary = this.summary();
            ClientUtil.sendPrefixed("Slayer: " + summary);
            if (config().summaryToCoopChat) {
                ChatCommandService.getInstance().send(ChatChannel.COOP, summary);
            }
        }
    }

    private void endQuest() {
        this.bossSpawnMs = 0;
        this.questStartMs = 0;
        this.questActiveMs = 0;
        this.killActiveMs = 0;
        this.progressLine = null;
    }

    /**
     * Advances the quest timers by the time since the last tick, unless the player is idle: no input for longer than
     * the idle pause, or (before the boss spawns) no quest progress for longer than the no progress pause. Idle
     * stretches count up to the pause, so short breaks are still part of the quest.
     *
     * @param input        whether the player gave input (moved, looked around, clicked) this tick
     * @param sidebarLines the sidebar lines if they were read this tick, else null
     */
    public void onTick(boolean input, @Nullable List<String> sidebarLines) {
        if (!config().enabled) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (input || this.lastInputMs == 0) {
            this.lastInputMs = now;
        }
        // a frozen or closed game doesn't count as questing
        final long delta = this.lastTickMs == 0 ? 0 : Math.max(0, Math.min(now - this.lastTickMs, 1_000));
        this.lastTickMs = now;
        if (!SlayerTracker.getInstance().isQuestActive()) {
            return;
        }
        if (sidebarLines != null) {
            this.updateProgress(sidebarLines, now);
        }
        final long idleMs = now - this.lastInputMs;
        if (idleMs <= config().profitIdlePauseMinutes * 60_000) {
            this.profitActiveMs += delta;
        }
        if (idleMs > config().idlePauseSeconds * 1_000) {
            return;
        }
        if (this.bossSpawnMs > 0) {
            if (!this.bossCocooned) {
                this.killActiveMs += delta;
            }
            this.questActiveMs += delta;
        } else if (!this.progressStalled(now)) {
            this.questActiveMs += delta;
        }
    }

    private void updateProgress(List<String> sidebarLines, long now) {
        for (final String line : sidebarLines) {
            if (QUEST_PROGRESS.matcher(line).matches()) {
                if (!line.equals(this.progressLine)) {
                    this.progressLine = line;
                    this.progressChangedMs = now;
                }
                return;
            }
        }
    }

    /** Only once a progress line was seen, so an unknown sidebar format doesn't pause the timer. */
    private boolean progressStalled(long now) {
        final float pauseSeconds = config().noProgressPauseSeconds;
        return pauseSeconds > 0 && this.progressLine != null && now - this.progressChangedMs > pauseSeconds * 1_000;
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
        final Stream<String> details = Stream.of(
                fields.averageKillTime ? averageTime("avg kill", stats.killTimeMs, stats.killTimed) : null,
                fields.averageQuestTime ? averageTime("avg quest", stats.questTimeMs, stats.questTimed) : null,
                fields.failed && stats.failed > 0 ? stats.failed + " failed" : null,
                fields.rngMeter ? this.rngMeterOf(boss) : null);
        return Stream.concat(Stream.of(headline(boss, stats, fields)), details.filter(Objects::nonNull))
                .collect(Collectors.joining(" | "));
    }

    /** E.g. "Inferno Demonlord IV: 42 bosses (31.2/h)". */
    private static String headline(String boss, BossStats stats, SlayerFieldsAccordion fields) {
        final String headline = boss + ": " + stats.kills + (stats.kills == 1 ? " boss" : " bosses");
        final double perHour = stats.perHour();
        return fields.bossesPerHour && !Double.isNaN(perHour)
                ? headline + String.format(Locale.ROOT, " (%.1f/h)", perHour)
                : headline;
    }

    private static @Nullable String averageTime(String label, long totalMs, int count) {
        return count > 0 ? label + " " + TimeUtil.formatMinutesSeconds(totalMs / count) : null;
    }

    private @Nullable String rngMeterOf(String boss) {
        final String slayer = slayerOf(boss);
        final Long xp = slayer == null ? null : this.rngMeter.get(slayer);
        return xp == null ? null : "RNG " + NumberUtil.formatCoins(xp) + " XP";
    }

    /**
     * Profit per boss and per hour of questing of the profit tracker's slayer activity, or null if it has none. The
     * tracker doesn't split by boss type, so this covers all slayers of the session.
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
        // per hour of questing as timed here, the tracker's own uptime knows nothing about idling during a quest
        if (this.profitActiveMs >= 60_000) {
            out.append(" (").append(formatSigned(stats.profit / (this.profitActiveMs / 3_600_000.0))).append("/h)");
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

    private static SlayerCategory config() {
        return SkysensConfig.getInstance().slayerCategory;
    }

}
