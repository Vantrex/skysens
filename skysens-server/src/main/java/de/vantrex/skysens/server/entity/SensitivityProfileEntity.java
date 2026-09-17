package de.vantrex.skysens.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A published sensitivity profile (§5.2). */
@Entity
@Table(name = "sensitivity_profile")
@Getter
@Setter
@NoArgsConstructor
public class SensitivityProfileEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // TODO(deferred): author FK, name, description, publishedAt, downloadCount,
    //                 and the serialized configuration payload.
}
