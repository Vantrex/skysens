/**
 * Contract interfaces mirroring the endpoint table in {@code ARCHITECTURE.md} §7.
 *
 * <p>These are plain Java interfaces on purpose. They carry no Spring annotations
 * (constraint 2) and no {@code ResponseEntity} — the server's controllers implement
 * them and add the HTTP mapping annotations on the implementing class, while the
 * mod's {@code client/net/} layer uses them purely as a compile-time reminder of
 * what the wire offers. Neither side can change a signature without breaking the
 * other, which is the whole reason this module exists.
 */
package de.vantrex.skysens.common.api;
