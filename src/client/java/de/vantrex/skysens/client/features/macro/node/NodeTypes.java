package de.vantrex.skysens.client.features.macro.node;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit registry of node types. Nothing is discovered by scanning; add a register(...) call per type. */
public final class NodeTypes {

    private static final Map<String, NodeType> TYPES = new LinkedHashMap<>();

    static {
        // No real node types yet (M2+).
    }

    private NodeTypes() {
    }

    @SuppressWarnings("unused")
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
