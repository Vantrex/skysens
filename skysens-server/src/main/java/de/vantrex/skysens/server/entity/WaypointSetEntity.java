package de.vantrex.skysens.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A published boss waypoint set (§5.3). */
@Entity
@Table(name = "waypoint_set")
@Getter
@Setter
@NoArgsConstructor
public class WaypointSetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // TODO(deferred): author FK, name, floor, hash, updatedAt, waypoint payload.
}
