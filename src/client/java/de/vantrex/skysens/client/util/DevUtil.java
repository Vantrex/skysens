package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@UtilityClass
public class DevUtil {

    private long PRINT_INTERVAL = 20_000;

    private long LAST_PRINT = -1;

    public void printScoreboardObjective(Scoreboard scoreboard, ScoreboardObjective sidebar) {
        if (LAST_PRINT + PRINT_INTERVAL > System.currentTimeMillis()) {
            return;
        }
        LAST_PRINT = System.currentTimeMillis();
        scoreboard.getScoreboardEntries(sidebar)
                        .stream()
                .map(scoreboardEntry -> scoreboard.getScoreHolderTeam(scoreboardEntry.owner()))
                .filter(Objects::nonNull)
                .forEach(team -> ClientUtil.sendDebug(teamToString(team)));
    }

    private String teamToString(Team team) {
        return "displayName=" + team.getDisplayName().getString() + ", prefix=" + team.getPrefix().getString() + ", suffix=" + team.getSuffix().getString();
    }
}
