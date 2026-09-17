package de.vantrex.skysens.client.feature.impl.dungeon;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.dungeon.enums.F7PhaseEnum;
import de.vantrex.skysens.client.dungeon.event.F7PhaseChangedEvent;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.feature.Feature;
import de.vantrex.skysens.client.util.ClientUtil;

public class BossWaypointsFeature implements Feature {

    public BossWaypointsFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this::onPhaseChange, F7PhaseChangedEvent.class);
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().dungeonCategory.bossWaypoints;
    }

    private void onPhaseChange(F7PhaseChangedEvent event) {
        if (!isActive()) return;
        
        // Logic to clear/set waypoints based on phase
        ClientUtil.sendDebug("BossWaypoints: Phase changed to " + event.getNewPhase());
        
        if (event.getNewPhase() == F7PhaseEnum.STORM) {
            // Start rendering Storm waypoints
        } else if (event.getNewPhase() == F7PhaseEnum.GOLDOR) {
            // Start rendering Goldor waypoints
        }
    }
}
