package de.vantrex.skysens.client.features.macro.action;

import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.PortKind;

import java.util.List;

/** A node that does something. Cooldown, rate limit and "close inventory first" are handled by the engine. */
public interface ActionNodeType extends NodeType {

    ActionOutcome execute(MacroNode node, MacroContext ctx);

    /** Whether this action sends to the server and so counts against the global rate limit. */
    default boolean sendsToServer() {
        return false;
    }

    @Override
    default NodeCategory category() {
        return NodeCategory.ACTION;
    }

    @Override
    default List<Port> inputs(MacroNode node) {
        return List.of(new Port("main", "", PortKind.INPUT));
    }

    @Override
    default List<Port> outputs(MacroNode node) {
        return List.of();
    }

    @Override
    default void validate(MacroNode node, List<NodeIssue> out) {
    }

}
