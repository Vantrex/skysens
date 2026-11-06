package de.vantrex.skysens.client.features.macro.node.logic;

import de.vantrex.skysens.client.features.macro.condition.RuleResult;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.features.macro.node.PortKind;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A node that picks an output port from conditions. */
public interface LogicNodeType extends NodeType {

    /**
     * @param port         the output port to follow, null if the path ends here
     * @param results      one result per rule group, in order (UNKNOWN ones carry their reason)
     * @param matchedPorts every output whose rule was TRUE (more than one = several rules matched)
     * @param unknown      the path ends because of an UNKNOWN rule, not because nothing matched
     */
    record Route(@Nullable String port, List<RuleResult> results, List<String> matchedPorts, boolean unknown) {
    }

    Route route(MacroNode node, MacroContext ctx);

    @Override
    default NodeCategory category() {
        return NodeCategory.LOGIC;
    }

    @Override
    default List<Port> inputs(MacroNode node) {
        return List.of(new Port("main", "", PortKind.INPUT));
    }

}
