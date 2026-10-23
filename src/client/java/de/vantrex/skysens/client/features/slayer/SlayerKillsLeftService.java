package de.vantrex.skysens.client.features.slayer;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.slayer.config.SlayerCategory;
import de.vantrex.skysens.client.skyblock.event.SlayerBossSpawnEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestEndEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestStartEvent;
import de.vantrex.skysens.client.skyblock.slayer.SlayerTracker;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Estimates the remaining miniboss kills from the slayer quest's combat XP progress. Called on the client thread. */
public final class SlayerKillsLeftService {

    private static final Pattern QUEST_PROGRESS = Pattern.compile("^\\(([\\d,.]+[kKmM]?)/([\\d,.]+[kKmM]?)\\) (.+)$");
    private static final SlayerKillsLeftService INSTANCE = new SlayerKillsLeftService();

    /** The smallest observed progress increase per kill for each boss and tier. */
    private final Map<String, Double> xpPerKill = new HashMap<>();
    private @Nullable Progress previousProgress;
    private @Nullable String actionBarMessage;
    private long actionBarUntilMs;

    private SlayerKillsLeftService() {
    }

    public static SlayerKillsLeftService getInstance() {
        return INSTANCE;
    }

    public void onQuestStart(SlayerQuestStartEvent event) {
        this.clearQuestProgress();
    }

    public void onBossSpawn(SlayerBossSpawnEvent event) {
        this.clearQuestProgress();
    }

    public void onQuestEnd(SlayerQuestEndEvent event) {
        this.clearQuestProgress();
    }

    /** Reads progress when available and refreshes our action bar every tick until it expires. */
    public void onTick(@Nullable List<String> sidebarLines) {
        final SlayerCategory category = SkysensConfig.getInstance().slayerCategory;
        if (!category.enabled || !category.killsLeft.actionBar) {
            this.previousProgress = null;
            this.clearActionBar();
            return;
        }

        if (sidebarLines != null && SlayerTracker.getInstance().isQuestActive() && !SlayerTracker.getInstance().isBossSpawned()) {
            this.updateProgress(sidebarLines, category);
        }

        if (this.actionBarMessage != null) {
            if (System.currentTimeMillis() >= this.actionBarUntilMs) {
                this.clearActionBar();
            } else {
                ClientUtil.sendActionBar(Component.literal(this.actionBarMessage));
            }
        }
    }

    private void updateProgress(List<String> sidebarLines, SlayerCategory category) {
        for (final String line : sidebarLines) {
            final Progress progress = parseProgress(line);
            if (progress == null) {
                continue;
            }
            if (this.previousProgress != null) {
                final double increase = progress.current - this.previousProgress.current;
                if (increase < 0) {
                    this.previousProgress = progress;
                    this.clearActionBar();
                    return;
                }
                if (increase > 0) {
                    this.showProgress(progress, increase, category);
                }
            }
            this.previousProgress = progress;
            return;
        }
    }

    private void showProgress(Progress progress, double increase, SlayerCategory category) {
        final String boss = SlayerTracker.getInstance().getBoss();
        final Double knownXpPerKill;
        if (boss == null) {
            knownXpPerKill = null;
        } else {
            this.xpPerKill.merge(boss, increase, Math::min);
            knownXpPerKill = this.xpPerKill.get(boss);
        }
        final long killsLeft = knownXpPerKill == null
                ? -1
                : (long) Math.ceil(Math.max(0, progress.required - progress.current) / knownXpPerKill);
        final String remaining = killsLeft < 0 ? "?" : Long.toString(killsLeft);
        this.actionBarMessage = "Slayer: " + formatXp(progress.current) + "/" + formatXp(progress.required) + " " + progress.label
                + " | ~" + remaining + " kills left";
        this.actionBarUntilMs = System.currentTimeMillis() + (long) (category.killsLeft.actionBarSeconds * 1_000);
    }

    private static @Nullable Progress parseProgress(String line) {
        final Matcher matcher = QUEST_PROGRESS.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new Progress(parseXp(matcher.group(1)), parseXp(matcher.group(2)), matcher.group(3));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static double parseXp(String value) {
        final String normalized = value.replace(",", "");
        final char suffix = Character.toLowerCase(normalized.charAt(normalized.length() - 1));
        final double multiplier = switch (suffix) {
            case 'k' -> 1_000;
            case 'm' -> 1_000_000;
            default -> 1;
        };
        final String number = multiplier == 1 ? normalized : normalized.substring(0, normalized.length() - 1);
        return Double.parseDouble(number) * multiplier;
    }

    private static String formatXp(double xp) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.round(xp));
    }

    private void clearQuestProgress() {
        this.previousProgress = null;
        this.clearActionBar();
    }

    private void clearActionBar() {
        this.actionBarMessage = null;
        this.actionBarUntilMs = 0;
    }

    private record Progress(double current, double required, String label) {
    }
}