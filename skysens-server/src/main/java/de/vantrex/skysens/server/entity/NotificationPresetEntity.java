package de.vantrex.skysens.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A published notification preset (§5.6). */
@Entity
@Table(name = "notification_preset")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPresetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // TODO(deferred): author FK, name, featureId, displayType, leadTimeMillis.
}
