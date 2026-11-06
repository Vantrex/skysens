package de.vantrex.skysens.client.features.macro.condition;

import org.jetbrains.annotations.Nullable;

/** Result of one rule, kept for the executions log. Runtime only. */
public record RuleResult(String ruleId, Tri value, @Nullable String unknownReason) {
}
