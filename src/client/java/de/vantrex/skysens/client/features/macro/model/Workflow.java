package de.vantrex.skysens.client.features.macro.model;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** One macro: nodes and the wires between them. The list order of workflows is the evaluation order. */
public class Workflow {

    public String id;
    public String name = "Macro";
    public boolean active = true;
    public ViewportState viewport = new ViewportState();
    public List<MacroNode> nodes = new ArrayList<>();
    public List<Connection> connections = new ArrayList<>();
    /** Unrecognised keys, kept so a save does not lose them. */
    public JsonObject extra = new JsonObject();

}
