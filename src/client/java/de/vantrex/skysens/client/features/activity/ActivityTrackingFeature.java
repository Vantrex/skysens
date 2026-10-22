package de.vantrex.skysens.client.features.activity;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.event.SkysensListener;
import de.vantrex.skysens.client.features.dungeon.DungeonRunCompleteEvent;
import de.vantrex.skysens.client.skyblock.event.LocationChangeEvent;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.util.TimeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Feeds the {@link ActivityTracker} from island changes, completed dungeon runs (see
 * {@link de.vantrex.skysens.client.features.dungeon.DungeonRunDetector}) and chat: "SLAYER QUEST COMPLETE!" (the boss comes from
 * "SLAYER BOSS! The X spawned!"), "KUUDRA DOWN!" and dug up Diana burrows ("You dug out a Griffin Burrow!").
 */
@SkySensFeature
public class ActivityTrackingFeature implements GameMessageListeningFeature, SkysensListener<LocationChangeEvent> {

    private static final Pattern SLAYER_SPAWN_PATTERN = Pattern.compile("^SLAYER BOSS! The (.+?) spawned!.*");
    private static final Pattern KUUDRA_TIME_PATTERN = Pattern.compile("^Time: ((?:\\d+h )?(?:\\d+m )?\\d+s).*");
    private static final Pattern BURROW_PATTERN = Pattern.compile("^(?:Woah! )?You dug out .*");

    private static final long KUUDRA_TIME_WINDOW_MS = 5_000L;
    private static final long MAX_SLAYER_MS = 60 * 60_000L;

    private String lastBoss;
    private long lastBossSpawn;
    private long kuudraDownAt;

    public ActivityTrackingFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this, LocationChangeEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(this::onDungeonRun, DungeonRunCompleteEvent.class);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ActivityTracker.getInstance().shutdown()));
    }

    @Override
    public void handle(LocationChangeEvent event) {
        if (this.isActive()) {
            ActivityTracker.getInstance().onLocationChange(event.getNewLocation());
        }
    }

    private void onDungeonRun(DungeonRunCompleteEvent event) {
        if (this.isActive()) {
            ActivityTracker.getInstance().record(ActivityType.DUNGEON, event.getFloor(), event.getSeconds() * 1_000L);
        }
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String raw = ChatFormatting.stripFormatting(message.getString());
        if (raw == null) {
            return;
        }
        final String text = raw.trim();
        final ActivityTracker tracker = ActivityTracker.getInstance();
        final long now = System.currentTimeMillis();

        final Matcher spawn = SLAYER_SPAWN_PATTERN.matcher(text);
        if (spawn.matches()) {
            this.lastBoss = spawn.group(1);
            this.lastBossSpawn = now;
            return;
        }
        if (text.startsWith("SLAYER QUEST COMPLETE!")) {
            final boolean known = this.lastBoss != null && now - this.lastBossSpawn <= MAX_SLAYER_MS;
            tracker.record(ActivityType.SLAYER, known ? this.lastBoss : "Slayer", known ? now - this.lastBossSpawn : 0);
            this.lastBoss = null;
            return;
        }
        if (text.startsWith("KUUDRA DOWN!")) {
            this.kuudraDownAt = now;
            return;
        }
        final Matcher kuudraTime = KUUDRA_TIME_PATTERN.matcher(text);
        if (kuudraTime.matches() && now - this.kuudraDownAt <= KUUDRA_TIME_WINDOW_MS) {
            this.kuudraDownAt = 0;
            tracker.record(ActivityType.KUUDRA, "Kuudra", TimeUtil.parseDuration(kuudraTime.group(1)));
            return;
        }
        if (BURROW_PATTERN.matcher(text).matches()) {
            tracker.record(ActivityType.DIANA, "Diana", 0);
        }
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.activity;
    }

}
