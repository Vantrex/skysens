package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@UtilityClass
public class DevUtil {

    private long PRINT_INTERVAL = 20_000;

    private long LAST_PRINT = -1;

    public void printScoreboardObjective(Scoreboard scoreboard, Objective sidebar) {
        if (LAST_PRINT + PRINT_INTERVAL > System.currentTimeMillis()) {
            return;
        }
        LAST_PRINT = System.currentTimeMillis();
        scoreboard.listPlayerScores(sidebar)
                        .stream()
                .map(scoreboardEntry -> scoreboard.getPlayersTeam(scoreboardEntry.owner()))
                .filter(Objects::nonNull)
                .forEach(team -> ClientUtil.sendDebug(teamToString(team)));
    }

    private String teamToString(PlayerTeam team) {
        return "displayName=" + team.getDisplayName().getString() + ", prefix=" + team.getPlayerPrefix().getString() + ", suffix=" + team.getPlayerSuffix().getString();
    }
}
