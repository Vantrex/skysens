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
import java.util.List;

@UtilityClass
public class ScoreboardUtil {

    /** The sidebar lines without formatting, in no particular order. Empty if there is no sidebar. */
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
        for (final PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
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
