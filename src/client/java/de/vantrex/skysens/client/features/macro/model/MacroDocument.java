package de.vantrex.skysens.client.features.macro.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Envelope used for the clipboard and templates. kind "workflows" carries {@link #workflows}; kind "nodes" carries a
 * canvas selection in {@link #nodes} and {@link #connections}. The repository stores only the workflow list.
 */
public class MacroDocument {

    public static final String KIND_WORKFLOWS = "workflows";
    public static final String KIND_NODES = "nodes";

    public String kind = KIND_WORKFLOWS;
    public List<Workflow> workflows = new ArrayList<>();
    public List<MacroNode> nodes = new ArrayList<>();
    public List<Connection> connections = new ArrayList<>();

}
