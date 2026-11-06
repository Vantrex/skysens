package de.vantrex.skysens.client.features.macro.engine;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** What one key press resolved to: every path walked, the chosen one (or -1) and what happened. */
public record Resolution(List<GraphWalker.PathResult> paths, int chosen, Outcome outcome, @Nullable String message) {

    public enum Outcome { RAN, RATE_LIMITED, ON_COOLDOWN, FAILED, NO_ACTION, NO_MATCH_UNKNOWN_STATE }

    public boolean ran() {
        return this.outcome == Outcome.RAN;
    }

    public @Nullable GraphWalker.PathResult chosenPath() {
        return this.chosen >= 0 ? this.paths.get(this.chosen) : null;
    }

}
