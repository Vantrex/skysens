package de.vantrex.skysens.client.features.macro.node;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;

import java.util.List;

/** A kind of node. Adding one = one class + one line in {@link NodeTypes}. */
public interface NodeType {

    /** Persisted as the node "type". Never rename. */
    String id();

    String displayName();

    NodeCategory category();

    /** Single glyph / short text drawn in the node. */
    String icon();

    List<ParamSpec> params();

    List<Port> inputs(MacroNode node);

    List<Port> outputs(MacroNode node);

    /** One-line subtitle on the canvas. */
    String summary(MacroNode node);

    void validate(MacroNode node, List<NodeIssue> out);

    default JsonObject defaultParams() {
        return ParamSpec.defaults(params());
    }

    default int width(MacroNode node) {
        return 120;
    }

}
