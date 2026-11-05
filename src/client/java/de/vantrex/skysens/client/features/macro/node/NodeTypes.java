package de.vantrex.skysens.client.features.macro.node;

import de.vantrex.skysens.client.features.macro.node.action.OpenUiNode;
import de.vantrex.skysens.client.features.macro.node.action.PlaySoundNode;
import de.vantrex.skysens.client.features.macro.node.action.SendChatNode;
import de.vantrex.skysens.client.features.macro.node.action.SendCommandNode;
import de.vantrex.skysens.client.features.macro.node.action.ToastNode;
import de.vantrex.skysens.client.features.macro.node.action.ToggleOptionNode;
import de.vantrex.skysens.client.features.macro.node.logic.IfNode;
import de.vantrex.skysens.client.features.macro.node.logic.SwitchNode;
import de.vantrex.skysens.client.features.macro.node.trigger.HotkeyTriggerNode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit registry of node types. Nothing is discovered by scanning; add a register(...) call per type. */
public final class NodeTypes {

    private static final Map<String, NodeType> TYPES = new LinkedHashMap<>();

    static {
        register(new HotkeyTriggerNode());
        register(new IfNode());
        register(new SwitchNode());
        register(new SendCommandNode());
        register(new SendChatNode());
        register(new ToastNode());
        register(new PlaySoundNode());
        register(new OpenUiNode());
        register(new ToggleOptionNode());
    }

    private NodeTypes() {
    }

    private static void register(NodeType type) {
        if (TYPES.put(type.id(), type) != null) {
            throw new IllegalStateException("Duplicate node type " + type.id());
        }
    }

    public static Collection<NodeType> all() {
        return TYPES.values();
    }

    public static boolean isKnown(String typeId) {
        return TYPES.containsKey(typeId);
    }

    /** The registered type, or an {@link UnknownNode} placeholder. */
    public static NodeType resolve(String typeId) {
        final NodeType type = TYPES.get(typeId);
        return type != null ? type : new UnknownNode(typeId);
    }

}
