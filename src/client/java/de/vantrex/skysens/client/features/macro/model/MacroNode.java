package de.vantrex.skysens.client.features.macro.model;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * One node of a workflow. The type is the persisted node type id; params are kept as raw JSON, so a node of an
 * unregistered type round-trips verbatim (type, params and unknown keys in {@link #extra}).
 */
public class MacroNode {

    public String id;
    public String type;
    public @Nullable String name;
    public boolean disabled;
    public int x;
    public int y;
    /** Size, only used by resizable nodes (sticky notes). 0 = default. */
    public int w;
    public int h;
    public JsonObject params = new JsonObject();
    /** Unrecognised keys, kept so a save does not lose them. */
    public JsonObject extra = new JsonObject();

}
