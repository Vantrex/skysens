package de.vantrex.skysens.client.features.macro.editor;

import de.vantrex.skysens.client.features.macro.model.Connection;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** What the canvas draws as "last run" for one workflow: nodes and wires per kind, and how strongly (fades out). */
public final class Highlight {

    public static final Highlight NONE = new Highlight(Map.of(), Map.of(), 0f);

    /** CHOSEN: the path that ran (green). ALSO: another path that matched too (amber). */
    public enum Kind { CHOSEN, ALSO }

    private final Map<String, Kind> nodes;
    private final Map<Connection, Kind> wires;
    private final float strength;

    private Highlight(Map<String, Kind> nodes, Map<Connection, Kind> wires, float strength) {
        this.nodes = nodes;
        this.wires = wires;
        this.strength = strength;
    }

    public static Builder builder(float strength) {
        return new Builder(strength);
    }

    /** 0..1; 1 right after the press (or while pinned). */
    public float strength() {
        return this.strength;
    }

    public boolean isEmpty() {
        return this.strength <= 0f || (this.nodes.isEmpty() && this.wires.isEmpty());
    }

    public @Nullable Kind node(String id) {
        return this.nodes.get(id);
    }

    public @Nullable Kind wire(Connection connection) {
        return this.wires.get(connection);
    }

    public static final class Builder {
        private final Map<String, Kind> nodes = new HashMap<>();
        private final Map<Connection, Kind> wires = new IdentityHashMap<>();
        private final float strength;

        private Builder(float strength) {
            this.strength = strength;
        }

        /** CHOSEN wins over ALSO when both paths share a node or wire. */
        public void node(String id, Kind kind) {
            this.nodes.merge(id, kind, (old, now) -> old == Kind.CHOSEN ? old : now);
        }

        public void wire(Connection connection, Kind kind) {
            this.wires.merge(connection, kind, (old, now) -> old == Kind.CHOSEN ? old : now);
        }

        public Highlight build() {
            return new Highlight(Collections.unmodifiableMap(this.nodes), Collections.unmodifiableMap(this.wires), this.strength);
        }
    }

}
