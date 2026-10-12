package de.vantrex.skysens.client.feature.dungeon;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.event.SkysensListener;
import de.vantrex.skysens.client.event.impl.DungeonRunCompleteEvent;
import de.vantrex.skysens.client.feature.Feature;
import de.vantrex.skysens.client.util.ClientUtil;

/** Records every completed dungeon run (see {@link DungeonRunDetector}) in the {@link DungeonRunStats}. */
public class DungeonRunStatsFeature implements Feature, SkysensListener<DungeonRunCompleteEvent> {

    public DungeonRunStatsFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this, DungeonRunCompleteEvent.class);
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().dungeonCategory.runStats.enabled;
    }

    @Override
    public void handle(DungeonRunCompleteEvent event) {
        if (!this.isActive()) {
            return;
        }
        final DungeonRunStats.FloorStats floorStats = DungeonRunStats.getInstance().record(event.getFloor(),
                event.getSeconds(), event.getScore(), event.getRank().equals("S+"), event.getDeaths());
        if (SkysensConfig.getInstance().dungeonCategory.runStats.chatSummary) {
            sendRunSummary(event, floorStats);
        }
    }

    private static void sendRunSummary(DungeonRunCompleteEvent event, DungeonRunStats.FloorStats floorStats) {
        ClientUtil.sendPrefixed("§7Run #" + floorStats.runs + " (" + event.getFloor() + "): "
                + DungeonRunStats.formatTime(event.getSeconds()) + " " + event.getRank()
                + " | session avg " + DungeonRunStats.formatTime(floorStats.avgSeconds()));
    }

}
