package de.vantrex.skysens.client.feature.dungeon;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.ItemIdUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records every completed dungeon run from Hypixel's end-of-run summary, one multi-line chat message:
 * <pre>
 *   Master Mode - M7
 *   Defeated Maxor, Storm, Goldor, and Necron in 07m 01s
 *   Score: 301 (S+)
 *   +514,710 Cata EXP ...
 * </pre>
 * Normal floors use "The Catacombs - F7" as header. The floor entry message ("Vantrex entered MM The Catacombs,
 * Floor VII!") sets the floor that reward chests opened in the instance count for. Own deaths ("☠ You were killed by Storm and became a ghost.")
 * are counted between the floor entry message and the summary.
 */
public class DungeonRunStatsFeature implements GameMessageListeningFeature {

    private static final Pattern HEADER = Pattern.compile("(?m)^\\s*(?:Master Mode|The Catacombs) - ([MF]\\d+)\\s*$");
    private static final Pattern TIME = Pattern.compile("(?m)^\\s*Defeated .+ in (?:(\\d+)h )?(?:(\\d+)m )?(\\d+)s\\s*$");
    private static final Pattern SCORE = Pattern.compile("(?m)^\\s*Score: (\\d+) \\(([A-Z+]+)\\)");
    private static final Pattern ENTERED = Pattern.compile("entered (MM )?The Catacombs, (?:Floor ([IVX]+)|Entrance)!");
    private static final Pattern OWN_DEATH = Pattern.compile("^\\s*☠ You (?:were killed by .+|died) and became a ghost\\.");

    private int deaths;

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().dungeonCategory.runStats.enabled;
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null || this.handleOwnDeath(text) || this.handleFloorEntered(text)) {
            return;
        }
        this.handleRunSummary(text);
    }

    private boolean handleOwnDeath(String text) {
        if (!OWN_DEATH.matcher(text).find()) {
            return false;
        }
        this.deaths++;
        return true;
    }

    private boolean handleFloorEntered(String text) {
        final Matcher entered = ENTERED.matcher(text);
        if (!entered.find()) {
            return false;
        }
        this.deaths = 0;
        // the reward chests of this instance count for its floor; the Entrance has no stats
        DungeonRunStats.getInstance().setCurrentFloor(entered.group(2) == null
                ? null
                : (entered.group(1) == null ? "F" : "M") + ItemIdUtil.romanToInt(entered.group(2)));
        return true;
    }

    private void handleRunSummary(String text) {
        final Matcher header = HEADER.matcher(text);
        final Matcher time = TIME.matcher(text);
        final Matcher score = SCORE.matcher(text);
        if (!header.find() || !time.find() || !score.find()) {
            return;
        }
        final String floor = header.group(1).toUpperCase(Locale.ROOT);
        final long seconds = parseSeconds(time);
        final String rank = score.group(2);
        final DungeonRunStats.FloorStats floorStats = DungeonRunStats.getInstance()
                .record(floor, seconds, Integer.parseInt(score.group(1)), rank.equals("S+"), this.deaths);
        this.deaths = 0;
        if (SkysensConfig.getInstance().dungeonCategory.runStats.chatSummary) {
            sendRunSummary(floor, seconds, rank, floorStats);
        }
    }

    /** Reads the "[Nh ][Nm ]Ns" run time of a matched {@link #TIME}. */
    private static long parseSeconds(Matcher time) {
        return groupOrZero(time, 1) * 3600 + groupOrZero(time, 2) * 60 + groupOrZero(time, 3);
    }

    private static long groupOrZero(Matcher matcher, int group) {
        final String value = matcher.group(group);
        return value == null ? 0 : Long.parseLong(value);
    }

    private static void sendRunSummary(String floor, long seconds, String rank, DungeonRunStats.FloorStats floorStats) {
        ClientUtil.sendPrefixed("§7Run #" + floorStats.runs + " (" + floor + "): " + DungeonRunStats.formatTime(seconds)
                + " " + rank + " | session avg " + DungeonRunStats.formatTime(floorStats.avgSeconds()));
    }

}
