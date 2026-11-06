package de.vantrex.skysens.client.features.macro.action;

import org.jetbrains.annotations.Nullable;

/** Result of trying to run one action node. */
public record ActionOutcome(Status status, @Nullable String message) {

    public enum Status { RAN, RATE_LIMITED, ON_COOLDOWN, FAILED, BLOCKED, WAITING }

    public static ActionOutcome ran() {
        return new ActionOutcome(Status.RAN, null);
    }

    public static ActionOutcome failed(String message) {
        return new ActionOutcome(Status.FAILED, message);
    }

    public static ActionOutcome rateLimited(long remainingMs) {
        return new ActionOutcome(Status.RATE_LIMITED, "rate limited, " + remainingMs + " ms left");
    }

    public static ActionOutcome onCooldown(long remainingMs) {
        return new ActionOutcome(Status.ON_COOLDOWN, "on cooldown, " + remainingMs + " ms left");
    }

    /** The command is held until the transfer cooldown is over; the press counts as handled. */
    public static ActionOutcome waiting(long remainingMs) {
        return new ActionOutcome(Status.WAITING, "WAITING for transfer cooldown, " + remainingMs + " ms left");
    }

    public boolean isWaiting() {
        return this.status == Status.WAITING;
    }

    public boolean isRan() {
        return this.status == Status.RAN;
    }

}
