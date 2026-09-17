package de.vantrex.skysens.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One submitted dungeon run (§5.1).
 */
@Entity
@Table(name = "dungeon_run")
@Getter
@Setter
@NoArgsConstructor
public class DungeonRunEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // TODO(deferred): player FK, floor, mode, startedAt, totalDurationMillis,
    //                 splitSetHash, and a child table for per-split durations.
    //                 Schema is owned by Flyway, not by ddl-auto.
}
