package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.ZoneEnum;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.scoreboard.*;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class LocationService {

    private static final int TICKS_UNTIL_ZONE_CHECK = 60;

    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final LocationService INSTANCE = new LocationService();

    private SkyblockLocationEnum currentLocation;
    private Zone<?> currentZone;
    private final SkysensClient skysens;
    private int zoneCheckTicksDown = 1;

    public LocationService() {
        this.skysens = SkysensClient.getInstance();
        this.init();
    }


    public static LocationService getInstance() {
        return INSTANCE;
    }

    private void updateZone(MinecraftClient client) {
        if (!this.skysens.isOnSkyBlock()) {
            return;
        }
        if (this.zoneCheckTicksDown-- > 0) {
            return;
        }
        this.zoneCheckTicksDown = TICKS_UNTIL_ZONE_CHECK;
        if (this.currentLocation == null) {
            return;
        }
        final ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        if (world.getScoreboard() == null || world.getScoreboard().getObjectives() == null) {
            return;
        }
        ScoreboardObjective objective = world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }
        this.updateZoneFromScoreboard(world.getScoreboard(), objective);
    }

    private void updateZoneFromScoreboard(Scoreboard scoreboard, ScoreboardObjective sidebar) {
        Zone<? extends ZoneEnum<?>> newZone = this.getZoneFromScoreboard(scoreboard, sidebar);
        final var oldZone = this.currentZone;
        if (zoneHasChanged(newZone)) {
            this.currentZone = newZone;
            ClientUtil.sendMessage("Zone changed to " + getZoneName(newZone));
        }
    }

    private boolean zoneHasChanged(final Zone<?> newZone) {
        if (this.currentZone == null && newZone != null) {
            return true;

        }
        if (this.currentZone != null && newZone == null) {
            return true;
        }
        if (this.currentZone == null) {
            return false;
        }
        return this.currentZone.getZoneEnum() != newZone.getZoneEnum();
    }

    private Zone<? extends ZoneEnum<?>> getZoneFromScoreboard(Scoreboard scoreboard, ScoreboardObjective sidebar) {
        for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(sidebar)) {
            Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
            if (team == null) {
                continue;
            }
            String line = team.getPrefix().getString() + team.getSuffix().getString();
            final Zone<? extends ZoneEnum<?>> zoneFromLine = Zone.fromScoreboardLine(line, this.currentLocation);
            if (zoneFromLine != null)
                return zoneFromLine;
        }
        return null;
    }

    public void handleLocationUpdate(@Nullable String map) {
        if (map == null) {
            this.currentLocation = null;
            return;
        }
        map = map.replace(" ", "_");
        SkyblockLocationEnum skyblockLocationEnum = getLocationFromMode(map);
        if (this.currentLocation != skyblockLocationEnum) {
            this.currentLocation = skyblockLocationEnum;
            ClientUtil.sendMessage("Location changed to " + getLocationName(skyblockLocationEnum));
            this.zoneCheckTicksDown = 25;
        }
    }

    private void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::updateZone);
    }


    public static String getLocationName(@Nullable SkyblockLocationEnum location) {
        return location == null ? "Unknown" : location.getDisplayName();
    }

    public static String getZoneName(@Nullable Zone<?> zone) {
        return zone == null ? "Unknown" : zone.getZoneEnum().getDisplayName();
    }

    private static SkyblockLocationEnum getLocationFromMode(String mode) {
        try {
            return SkyblockLocationEnum.valueOf(mode.toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

}
