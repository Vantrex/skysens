package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.skyblock.sidebar.SidebarEventTracker;
import de.vantrex.skysens.client.skyblock.sidebar.SidebarEventType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Events read from the sidebar. Everything is UNKNOWN until the sidebar was read since the last join or island change. */
public final class SidebarFacts {

    private final SidebarEventTracker tracker = SidebarEventTracker.getInstance();

    public boolean known() {
        return this.tracker.isKnown();
    }

    public Tri timedEventActive(SidebarEventType type) {
        return this.tracker.getEventActive(type).map(Tri::of).orElse(Tri.UNKNOWN);
    }

    /** The mining event as shown, null if none (check {@link #known()} first). */
    public @Nullable String miningEvent() {
        return this.tracker.getMiningEvent();
    }

    public Tri jacobsActive() {
        return this.tracker.getJacobsContestActive().map(Tri::of).orElse(Tri.UNKNOWN);
    }

    public Optional<String> jacobsCrop() {
        return this.tracker.getJacobsCrop();
    }

    /** 1-5 in Kuudra, 0 otherwise. */
    public int kuudraTier() {
        return this.tracker.getKuudraTier();
    }

}
