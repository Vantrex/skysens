package de.vantrex.skysens.client.features.slayer;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.slayer.config.SlayerCategory;
import de.vantrex.skysens.client.skyblock.event.SlayerBossSpawnEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestEndEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestStartEvent;
import de.vantrex.skysens.client.notification.SecondaryActionBar;
import de.vantrex.skysens.client.skyblock.slayer.SlayerTracker;
import de.vantrex.skysens.client.util.NumberUtil;
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
    /** The estimate last sent to the secondary action bar, so it replaces or hides only its own message. */
    private @Nullable Component shownEstimate;

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

    /** Reads the quest progress from the sidebar when it is given. */
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
        final Component estimate = Component.literal("Slayer: " + formatXp(progress.current) + "/"
                + formatXp(progress.required) + " " + progress.label + " | ~" + remaining + " kills left");
        // overrides the previous estimate in place, shown or still waiting; other messages such as the RNG meter
        // progress still wait on it, and it waits on them
        SecondaryActionBar.getInstance().replace(this.shownEstimate, estimate,
                (long) (category.killsLeft.actionBarSeconds * 1_000));
        this.shownEstimate = estimate;
    }

    private static @Nullable Progress parseProgress(String line) {
        final Matcher matcher = QUEST_PROGRESS.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new Progress(NumberUtil.parseAbbreviated(matcher.group(1)), NumberUtil.parseAbbreviated(matcher.group(2)),
                    matcher.group(3));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatXp(double xp) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.round(xp));
    }

    private void clearQuestProgress() {
        this.previousProgress = null;
        this.clearActionBar();
    }

    private void clearActionBar() {
        SecondaryActionBar.getInstance().clear(this.shownEstimate);
        this.shownEstimate = null;
    }

    private record Progress(double current, double required, String label) {
    }
}