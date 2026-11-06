package de.vantrex.skysens.client.features.macro.condition;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;

import java.util.List;

/**
 * A kind of condition used inside If / Switch rules. The tri-state rules (negation, AND/OR, empty group) are applied
 * by {@link ConditionRule} and {@link RuleGroup}, never in here: an implementation only reports TRUE, FALSE or
 * UNKNOWN (with a reason) for its own params.
 */
public interface ConditionType {

    /** Prefix of a validate() problem that is only a WARNING; every other problem is an ERROR. */
    String WARN = "warn:";

    /** Persisted as the rule "type". Never rename. */
    String id();

    String displayName();

    /** Picker grouping: "Location", "Items", "Party", ... */
    String group();

    List<ParamSpec> params();

    /** Call {@code reasons.unknown(...)} whenever the result is UNKNOWN. */
    Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons);

    /** Human readable form, e.g. "island is Hub or Dwarven Mines". */
    String describe(JsonObject params);

    default void validate(JsonObject params, List<String> problems) {
    }

    /** Whether the state this condition reads comes from a tracker that can be unknown (for editor badges). */
    default boolean trackerBacked() {
        return false;
    }

    default String canonicalKey(JsonObject params) {
        return id() + ":" + Params.canonical(params);
    }

}
