package de.vantrex.skysens.client.features.macro.node;

import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;

import java.util.List;

/**
 * Placeholder for a node whose type is not registered (made by a newer SkySens, or a typo). The node itself keeps its
 * type, params and unknown keys verbatim, so saving never loses it; this class only describes it.
 */
public final class UnknownNode implements NodeType {

    private final String typeId;

    public UnknownNode(String typeId) {
        this.typeId = typeId;
    }

    @Override
    public String id() {
        return this.typeId;
    }

    @Override
    public String displayName() {
        return "Unknown (" + this.typeId + ")";
    }

    @Override
    public NodeCategory category() {
        return NodeCategory.UNKNOWN;
    }

    @Override
    public String icon() {
        return "?";
    }

    @Override
    public List<ParamSpec> params() {
        return List.of();
    }

    @Override
    public List<Port> inputs(MacroNode node) {
        return List.of(new Port("main", "", PortKind.INPUT));
    }

    @Override
    public List<Port> outputs(MacroNode node) {
        return List.of();
    }

    @Override
    public String summary(MacroNode node) {
        return "Unknown node type";
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        out.add(new NodeIssue(NodeIssue.Severity.ERROR, node.id, "Unknown node type \"" + this.typeId + "\" (made with a newer SkySens?). It is kept as is but never runs"));
    }

}
