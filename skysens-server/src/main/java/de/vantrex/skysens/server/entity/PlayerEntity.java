package de.vantrex.skysens.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * The canonical user record. The primary key is the Mojang UUID — the only
 * identity the server ever trusts (§6).
 *
 * <p>Entities are kept deliberately separate from the {@code skysens-common} DTOs:
 * the wire contract and the storage schema must be free to evolve independently,
 * and a JPA entity can never be put on the mod's classpath.
 */
@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
public class PlayerEntity {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    /**
     * Last known display name. Cached for presentation only — mutable, spoofable,
     * and never used for authorization.
     */
    @Column(name = "last_known_name")
    private String lastKnownName;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    // TODO(deferred): opt-in flags mirroring the client-side SkysensConfig toggles,
    //                 so a server-side revocation survives a client reinstall.
}
