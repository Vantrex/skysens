package de.vantrex.skysens.client.skyblock.slayer;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.skyblock.event.SlayerBossCocoonEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerBossSpawnEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestEndEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestStartEvent;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ScoreboardUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tells whether a slayer quest is running, which boss it is for and whether the boss has spawned or was cocooned, and
 * fires {@link SlayerQuestStartEvent}, {@link SlayerBossSpawnEvent}, {@link SlayerBossCocoonEvent} and
 * {@link SlayerQuestEndEvent}.
 * Chat ("SLAYER QUEST STARTED!", "SLAYER BOSS! The X spawned!", "YOU COCOONED YOUR SLAYER BOSS",
 * "SLAYER QUEST COMPLETE!", "SLAYER QUEST FAILED!") reacts at once; the sidebar ("Slayer Quest", the boss line below it, "Slay the boss!") is read once a second and
 * catches what chat misses: a quest that was running before joining, or one cancelled at Maddox.
 * Always active, independent of the slayer stats in {@code features/slayer}. Called on the client thread.
 */
public class SlayerTracker implements GameMessageListeningFeature, AfterTickFeature {

    @Getter
    private static final SlayerTracker instance = new SlayerTracker();

    private static final Pattern BOSS_SPAWN = Pattern.compile("^SLAYER BOSS! The (.+) spawned!$");
    private static final String SIDEBAR_QUEST = "Slayer Quest";
    private static final String SIDEBAR_BOSS_SPAWNED = "Slay the boss!";
    /** The sidebar is read once a second, the quest state doesn't need more. */
    private static final int SIDEBAR_INTERVAL_TICKS = 20;
    /** Right after the start or end message the sidebar may not be updated yet. */
    private static final long SIDEBAR_GRACE_MS = 5_000L;

    private volatile boolean questActive;
    private volatile boolean bossSpawned;
    /** The boss was cocooned: no longer fighting, the quest completes once the cocoon hatches. */
    private volatile boolean bossCocooned;
    /** The boss with its tier (e.g. "Revenant Horror IV"), from the spawn message or the sidebar. */
    private volatile @Nullable String boss;
    private long questSeenSinceMs;
    private long questEndedByChatMs;
    private int ticks;

    private SlayerTracker() {
    }

    public boolean isQuestActive() {
        return this.questActive;
    }

    public boolean isBossSpawned() {
        return this.bossSpawned;
    }

    public boolean isBossCocooned() {
        return this.bossCocooned;
    }

    public @Nullable String getBoss() {
        return this.boss;
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) {
            return;
        }
        final String line = ChatUtil.normalize(text);
        if (line.equals("SLAYER QUEST STARTED!")) {
            this.startQuest(true);
            return;
        }
        final Matcher spawn = BOSS_SPAWN.matcher(line);
        if (spawn.matches()) {
            if (!this.questActive) {
                this.startQuest(false);
            }
            this.boss = spawn.group(1);
            if (!this.bossSpawned) {
                this.bossSpawned();
            }
            return;
        }
        if (line.equals("YOU COCOONED YOUR SLAYER BOSS")) {
            if (this.questActive && !this.bossCocooned) {
                this.bossCocooned = true;
                EventListenerRegistry.REGISTRY.fireEvent(new SlayerBossCocoonEvent());
            }
            return;
        }
        // ends the quest even when its start wasn't seen, so the kill still counts
        if (line.equals("SLAYER QUEST COMPLETE!")) {
            this.endQuest(SlayerQuestEndEvent.Reason.COMPLETED);
            this.questEndedByChatMs = System.currentTimeMillis();
        } else if (line.equals("SLAYER QUEST FAILED!")) {
            this.endQuest(SlayerQuestEndEvent.Reason.FAILED);
            this.questEndedByChatMs = System.currentTimeMillis();
        }
    }

    @Override
    public void afterTick() {
        if (++this.ticks % SIDEBAR_INTERVAL_TICKS != 0 || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final List<String> sidebar = ScoreboardUtil.getSidebarLines();
        if (sidebar.isEmpty()) {
            // server switch: keep the state until the sidebar is back
            return;
        }
        final long now = System.currentTimeMillis();
        final int questLine = this.indexOfQuestLine(sidebar);
        if (questLine < 0) {
            if (this.questActive && now - this.questSeenSinceMs >= SIDEBAR_GRACE_MS) {
                this.endQuest(SlayerQuestEndEvent.Reason.CANCELLED);
            }
            return;
        }
        if (!this.questActive) {
            if (now - this.questEndedByChatMs < SIDEBAR_GRACE_MS) {
                // the sidebar still shows the quest that just ended
                return;
            }
            this.startQuest(false);
        }
        if (questLine + 1 < sidebar.size()) {
            this.boss = sidebar.get(questLine + 1).trim();
        }
        if (!this.bossSpawned && sidebar.stream().anyMatch(line -> line.startsWith(SIDEBAR_BOSS_SPAWNED))) {
            this.bossSpawned();
        }
    }

    private int indexOfQuestLine(List<String> sidebar) {
        for (int i = 0; i < sidebar.size(); i++) {
            if (sidebar.get(i).startsWith(SIDEBAR_QUEST)) {
                return i;
            }
        }
        return -1;
    }

    private void startQuest(boolean startSeen) {
        this.questActive = true;
        this.bossSpawned = false;
        this.bossCocooned = false;
        this.boss = null;
        this.questSeenSinceMs = System.currentTimeMillis();
        EventListenerRegistry.REGISTRY.fireEvent(new SlayerQuestStartEvent(startSeen));
    }

    private void bossSpawned() {
        this.bossSpawned = true;
        EventListenerRegistry.REGISTRY.fireEvent(new SlayerBossSpawnEvent(this.boss));
    }

    private void endQuest(SlayerQuestEndEvent.Reason reason) {
        this.questActive = false;
        this.bossSpawned = false;
        this.bossCocooned = false;
        this.boss = null;
        EventListenerRegistry.REGISTRY.fireEvent(new SlayerQuestEndEvent(reason));
    }

    @Override
    public boolean isActive() {
        return true;
    }

}
