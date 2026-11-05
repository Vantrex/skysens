package de.vantrex.skysens.client.features.macro.analysis;

/** A problem with a node, shown as a badge in the editor and counted by /skysens macros debug. */
public record NodeIssue(Severity severity, String nodeId, String message) {

    public enum Severity { ERROR, WARNING, INFO }

}
