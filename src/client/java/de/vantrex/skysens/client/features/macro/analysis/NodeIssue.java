package de.vantrex.skysens.client.features.macro.analysis;

/** A problem with a node, shown as a badge in the editor and counted by /skysens macros debug. */
public record NodeIssue(Severity severity, String nodeId, String message, Kind kind) {

    public enum Severity { ERROR, WARNING, INFO }

    /** KEYBIND issues feed the header chip of the editor. */
    public enum Kind { GENERAL, KEYBIND }

    public NodeIssue(Severity severity, String nodeId, String message) {
        this(severity, nodeId, message, Kind.GENERAL);
    }

}
