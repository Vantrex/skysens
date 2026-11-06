package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.skyblock.slayer.SlayerTracker;
import org.jetbrains.annotations.Nullable;

/** The slayer quest. UNKNOWN until the sidebar was read since the last join or island change. */
public final class SlayerFacts {

    private final SlayerTracker tracker = SlayerTracker.getInstance();

    public boolean known() {
        return this.tracker.isSidebarKnown();
    }

    public Tri questActive() {
        return this.known() ? Tri.of(this.tracker.isQuestActive()) : Tri.UNKNOWN;
    }

    public Tri bossSpawned() {
        return this.known() ? Tri.of(this.tracker.isQuestActive() && this.tracker.isBossSpawned()) : Tri.UNKNOWN;
    }

    /** The boss name without tier, null if no quest or the boss line isn't parsed yet. */
    public @Nullable String bossName() {
        return this.tracker.isQuestActive() ? this.tracker.getBossName() : null;
    }

    /** 1-5, 0 if no quest or not known yet. */
    public int tier() {
        return this.tracker.isQuestActive() ? this.tracker.getBossTier() : 0;
    }

}
