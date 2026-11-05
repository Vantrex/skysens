package de.vantrex.skysens.client.features.macro.condition;

/** Collects why a condition was UNKNOWN. Runtime only. */
public interface ReasonSink {

    void unknown(String reason);

}
