package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.ZoneEnum;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.event.impl.LocationChangeEvent;
import de.vantrex.skysens.client.event.impl.ZoneChangeEvent;
import de.vantrex.skysens.client.model.Zone;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class LocationService {

    private static final int TICKS_UNTIL_ZONE_CHECK = 60;
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

    private void updateZone(Minecraft client) {
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
        final ClientLevel world = client.level;
        if (world == null) {
            return;
        }
        if (world.getScoreboard() == null || world.getScoreboard().getObjectives() == null) {
            return;
        }
        Objective objective = world.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }
        this.updateZoneFromScoreboard(world.getScoreboard(), objective);
    }

    private void updateZoneFromScoreboard(Scoreboard scoreboard, Objective sidebar) {
        Zone<? extends ZoneEnum<?>> newZone = this.getZoneFromScoreboard(scoreboard, sidebar);
        final var oldZone = this.currentZone;
        if (zoneHasChanged(newZone)) {
            this.currentZone = newZone;
            ZoneChangeEvent event = new ZoneChangeEvent((Zone<? extends ZoneEnum<?>>) oldZone, newZone);
            EventListenerRegistry.REGISTRY.fireEvent(event);
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

    private Zone<? extends ZoneEnum<?>> getZoneFromScoreboard(Scoreboard scoreboard, Objective sidebar) {
        for (PlayerScoreEntry scoreboardEntry : scoreboard.listPlayerScores(sidebar)) {
            PlayerTeam team = scoreboard.getPlayersTeam(scoreboardEntry.owner());
            if (team == null) {
                continue;
            }
            String line = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
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
            final var oldLocation = this.currentLocation;
            this.currentLocation = skyblockLocationEnum;
            LocationChangeEvent locationChangeEvent = new LocationChangeEvent(oldLocation, skyblockLocationEnum);
            EventListenerRegistry.REGISTRY.fireEvent(locationChangeEvent);
            final var oldZone = this.currentZone;
            this.currentZone = null;
            ZoneChangeEvent zoneChangeEvent = new ZoneChangeEvent((Zone<? extends ZoneEnum<?>>) oldZone, null);
            EventListenerRegistry.REGISTRY.fireEvent(zoneChangeEvent);
            ClientUtil.sendDebug("Location changed to " + getLocationName(skyblockLocationEnum));
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
