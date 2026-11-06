package de.vantrex.skysens.client.features.macro.node;

/** A connection point. The id is persisted in connections ("main", "true", "rule:id", ...). */
public record Port(String id, String label, PortKind kind) {
}
