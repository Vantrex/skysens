package de.vantrex.skysens.client.features.dungeon;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.skyblock.item.ItemIdUtil;
import lombok.Getter;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects completed dungeon runs from the end-of-run summary and fires a {@link DungeonRunCompleteEvent} once per run.
 * The summary is read line by line, a header, then the boss line, then the score. Two formats are understood:
 * <ul>
 *   <li>Hypixel's, one message per line: "Master Mode The Catacombs - Floor VII", "Defeated Maxor, Storm, Goldor,
 *   and Necron in 07m 01s", "Team Score: 301 (S+)".</li>
 *   <li>Odin's "Extra Stats" replacement, one message: "Master Mode - M7", "Defeated ... in 07m 01s",
 *   "Score: 301 (S+)".</li>
 * </ul>
 * Odin hides Hypixel's lines, so cancelled messages are read too, and Odin's own message never fires a game message
 * event, so it comes in through {@code ChatComponentMixin}. When both formats are seen for one run, only the first
 * counts. The floor entry message ("Vantrex entered MM The Catacombs, Floor VII!") sets the floor that reward chests
 * opened in the instance count for. Own deaths ("☠ You were killed by Storm and became a ghost.") are counted
 * between the floor entry message and the summary.
 */
public class DungeonRunDetector implements GameMessageListeningFeature {

    @Getter
    private static final DungeonRunDetector instance = new DungeonRunDetector();

    /** Hypixel's header, "The Catacombs - Floor VII", "Master Mode The Catacombs - Floor VII" or "... - Entrance". */
    private static final Pattern HYPIXEL_HEADER = Pattern.compile("^\\s*(Master Mode )?(?:The )?Catacombs - (?:Entrance|Floor ([IVX]+))\\s*$");
    /** Odin's header, "Master Mode - M7" or "The Catacombs - F7". */
    private static final Pattern ODIN_HEADER = Pattern.compile("^\\s*(?:Master Mode|The Catacombs) - ([MF]\\d+)\\s*$");
    private static final Pattern TIME = Pattern.compile("^\\s*Defeated .+ in (?:(\\d+)h )?(?:(\\d+)m )?(\\d+)s(?:\\s*\\(NEW RECORD!\\))?\\s*$");
    private static final Pattern SCORE = Pattern.compile("^\\s*(?:Team )?Score: (\\d+) \\(([A-Z+]+)\\)");
    private static final Pattern ENTERED = Pattern.compile("entered (MM )?The Catacombs, (?:Floor ([IVX]+)|Entrance)!");
    private static final Pattern OWN_DEATH = Pattern.compile("^\\s*☠ You (?:were killed by .+|died) and became a ghost\\.");
    /** No two runs end this close together, so a second summary within it is the same run in another format. */
    private static final long SAME_RUN_MS = 60_000L;

    private final boolean odinLoaded = FabricLoader.getInstance().isModLoaded("odin");

    /** The floor of the summary being read, null when no summary (or one of the Entrance) is being read. */
    private @Nullable String pendingFloor;
    private long pendingSeconds = -1;
    private int deaths;
    private long lastRunAt;

    private DungeonRunDetector() {
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> {
            if (!ClientUtil.isSendingOwnMessage()) {
                this.onGameMessage(message, overlay);
            }
        });
    }

    @Override
    public boolean isActive() {
        final SkysensConfig config = SkysensConfig.getInstance();
        return config.dungeonCategory.runStats.enabled || config.chatCommandsCategory.commands.activity;
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (!overlay && this.isActive()) {
            this.handle(message);
        }
    }

    /** A message added to the chat directly, without a game message event. Only Odin's run summary is of interest. */
    public void onClientSystemMessage(Component message) {
        if (this.odinLoaded && this.isActive() && !ClientUtil.isSendingOwnMessage()) {
            this.handle(message);
        }
    }

    private synchronized void handle(Component message) {
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) {
            return;
        }
        for (final String line : text.split("\n")) {
            this.handleLine(line);
        }
    }

    private void handleLine(String line) {
        if (this.handleOwnDeath(line) || this.handleFloorEntered(line) || this.handleHeader(line)
                || this.pendingFloor == null || this.handleTime(line)) {
            return;
        }
        this.handleScore(line);
    }

    private boolean handleOwnDeath(String line) {
        if (!OWN_DEATH.matcher(line).find()) {
            return false;
        }
        this.deaths++;
        return true;
    }

    private boolean handleFloorEntered(String line) {
        final Matcher entered = ENTERED.matcher(line);
        if (!entered.find()) {
            return false;
        }
        this.deaths = 0;
        this.pendingFloor = null;
        this.lastRunAt = 0;
        // the reward chests of this instance count for its floor
        DungeonRunStats.getInstance().setCurrentFloor(floorName(entered.group(1) != null, entered.group(2)));
        return true;
    }

    /** Starts reading a summary at its header, in Hypixel's or Odin's format. */
    private boolean handleHeader(String line) {
        final Matcher hypixel = HYPIXEL_HEADER.matcher(line);
        if (hypixel.matches()) {
            this.startSummary(floorName(hypixel.group(1) != null, hypixel.group(2)));
            return true;
        }
        final Matcher odin = ODIN_HEADER.matcher(line);
        if (odin.matches()) {
            this.startSummary(odin.group(1).toUpperCase(Locale.ROOT));
            return true;
        }
        return false;
    }

    private void startSummary(@Nullable String floor) {
        this.pendingFloor = floor;
        this.pendingSeconds = -1;
    }

    private boolean handleTime(String line) {
        final Matcher time = TIME.matcher(line);
        if (!time.matches()) {
            return false;
        }
        this.pendingSeconds = parseSeconds(time);
        return true;
    }

    private void handleScore(String line) {
        final Matcher score = SCORE.matcher(line);
        if (score.find() && this.pendingSeconds >= 0) {
            this.completeRun(Integer.parseInt(score.group(1)), score.group(2));
        }
    }

    private void completeRun(int score, String rank) {
        final String floor = this.pendingFloor;
        this.pendingFloor = null;
        if (this.isSameRunAsLast()) {
            return;
        }
        final int deaths = this.deaths;
        this.deaths = 0;
        EventListenerRegistry.REGISTRY.fireEvent(new DungeonRunCompleteEvent(floor, this.pendingSeconds, score, rank, deaths));
    }

    /** Whether a run was completed moments ago, i.e. this summary is the same run in the other format. */
    private boolean isSameRunAsLast() {
        final long now = System.currentTimeMillis();
        if (now - this.lastRunAt < SAME_RUN_MS) {
            return true;
        }
        this.lastRunAt = now;
        return false;
    }

    /** "M7" / "F5" from the roman floor number, null for the Entrance (it has no stats). */
    private static @Nullable String floorName(boolean masterMode, @Nullable String romanFloor) {
        return romanFloor == null ? null : (masterMode ? "M" : "F") + ItemIdUtil.romanToInt(romanFloor);
    }

    /** Reads the "[Nh ][Nm ]Ns" run time of a matched {@link #TIME}. */
    private static long parseSeconds(Matcher time) {
        return groupOrZero(time, 1) * 3600 + groupOrZero(time, 2) * 60 + groupOrZero(time, 3);
    }

    private static long groupOrZero(Matcher matcher, int group) {
        final String value = matcher.group(group);
        return value == null ? 0 : Long.parseLong(value);
    }

}
