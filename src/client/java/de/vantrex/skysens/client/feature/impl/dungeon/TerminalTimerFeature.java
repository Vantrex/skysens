package de.vantrex.skysens.client.feature.impl.dungeon;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.dungeon.enums.F7PhaseEnum;
import de.vantrex.skysens.client.dungeon.event.F7PhaseChangedEvent;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.feature.Feature;
import de.vantrex.skysens.client.util.ClientUtil;

public class TerminalTimerFeature implements Feature {

    public TerminalTimerFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this::onPhaseChange, F7PhaseChangedEvent.class);
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().dungeonCategory.terminalTimer;
    }

    private void onPhaseChange(F7PhaseChangedEvent event) {
        if (!isActive()) return;
        
        if (event.getNewPhase() == F7PhaseEnum.NECRON) {
            ClientUtil.sendDebug("TerminalTimer: Preparing for Necron phase terminals");
            // Start terminal logic
        }
    }
}
