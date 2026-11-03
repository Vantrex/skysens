package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class ScoreboardUtil {

    /** The order vanilla draws the sidebar in: highest score at the top, ties by owner name. */
    private final Comparator<PlayerScoreEntry> DISPLAY_ORDER = Comparator.comparingInt(PlayerScoreEntry::value).reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    /** The sidebar lines without formatting, top to bottom as they are shown. Empty if there is no sidebar. */
    public List<String> getSidebarLines() {
        final List<String> lines = new ArrayList<>();
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return lines;
        }
        final Scoreboard scoreboard = level.getScoreboard();
        final Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return lines;
        }
        final List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(sidebar));
        entries.removeIf(PlayerScoreEntry::isHidden);
        entries.sort(DISPLAY_ORDER);
        for (final PlayerScoreEntry entry : entries) {
            final PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            if (team == null) {
                continue;
            }
            final String line = ChatFormatting.stripFormatting(team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString());
            if (line != null) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

}
