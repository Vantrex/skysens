package de.vantrex.skysens.client.feature.activity;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.activity.ActivityTracker;
import de.vantrex.skysens.client.activity.ActivityType;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.event.SkysensListener;
import de.vantrex.skysens.client.event.impl.LocationChangeEvent;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Feeds the {@link ActivityTracker} from island changes and chat: dungeon end summaries
 * ("Master Mode - M7 ... Defeated Maxor, ... in 07m 01s"), "SLAYER QUEST COMPLETE!" (the boss comes from
 * "SLAYER BOSS! The X spawned!"), "KUUDRA DOWN!" and dug up Diana burrows ("You dug out a Griffin Burrow!").
 */
@SkySensFeature
public class ActivityTrackingFeature implements GameMessageListeningFeature, SkysensListener<LocationChangeEvent> {

    /** "Master Mode - M7" / "The Catacombs - F5", followed somewhere by "Defeated ... in 07m 01s". */
    private static final Pattern DUNGEON_PATTERN = Pattern.compile("(?:Master Mode|The Catacombs) - ([FM]\\d)\\b.*?Defeated .*? in ((?:\\d+h )?(?:\\d+m )?\\d+s)", Pattern.DOTALL);
    private static final Pattern SLAYER_SPAWN_PATTERN = Pattern.compile("^SLAYER BOSS! The (.+?) spawned!.*");
    private static final Pattern KUUDRA_TIME_PATTERN = Pattern.compile("^Time: ((?:\\d+h )?(?:\\d+m )?\\d+s).*");
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([hms])");
    private static final Pattern BURROW_PATTERN = Pattern.compile("^(?:Woah! )?You dug out .*");

    private static final long KUUDRA_TIME_WINDOW_MS = 5_000L;
    private static final long MAX_SLAYER_MS = 60 * 60_000L;

    private String lastBoss;
    private long lastBossSpawn;
    private long kuudraDownAt;

    public ActivityTrackingFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this, LocationChangeEvent.class);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ActivityTracker.getInstance().shutdown()));
    }

    @Override
    public void handle(LocationChangeEvent event) {
        if (this.isActive()) {
            ActivityTracker.getInstance().onLocationChange(event.getNewLocation());
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

        if (text.contains("Defeated")) {
            final Matcher dungeon = DUNGEON_PATTERN.matcher(text);
            if (dungeon.find()) {
                tracker.record(ActivityType.DUNGEON, dungeon.group(1), parseDuration(dungeon.group(2)));
                return;
            }
        }
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
            tracker.record(ActivityType.KUUDRA, "Kuudra", parseDuration(kuudraTime.group(1)));
            return;
        }
        if (BURROW_PATTERN.matcher(text).matches()) {
            tracker.record(ActivityType.DIANA, "Diana", 0);
        }
    }

    /** Parses "1h 02m 03s" style durations into milliseconds. */
    static long parseDuration(String text) {
        long millis = 0;
        final Matcher matcher = DURATION_PART.matcher(text);
        while (matcher.find()) {
            final long value = Long.parseLong(matcher.group(1));
            millis += switch (matcher.group(2)) {
                case "h" -> value * 3_600_000L;
                case "m" -> value * 60_000L;
                default -> value * 1_000L;
            };
        }
        return millis;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.activity;
    }

}
