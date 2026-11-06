package de.vantrex.skysens.client.features.macro.model;

import com.google.gson.JsonObject;

/** A wire from an output port of one node to the input of another. Each output port holds at most one wire. */
public class Connection {

    public String from;
    public String port = "main";
    public String to;
    /** Unrecognised keys, kept so a save does not lose them. */
    public JsonObject extra = new JsonObject();

    public Connection() {
    }

    public Connection(String from, String port, String to) {
        this.from = from;
        this.port = port;
        this.to = to;
    }

}
