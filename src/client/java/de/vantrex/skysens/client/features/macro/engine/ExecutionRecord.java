package de.vantrex.skysens.client.features.macro.engine;

/** One logged key press that had at least one trigger bound to it. */
public record ExecutionRecord(long time, InputChord chord, PressMode mode, boolean inGui, Resolution resolution) {
}
