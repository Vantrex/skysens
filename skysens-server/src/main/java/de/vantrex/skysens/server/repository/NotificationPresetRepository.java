package de.vantrex.skysens.server.repository;

import de.vantrex.skysens.server.entity.NotificationPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Persistence for {@link NotificationPresetEntity}.
 */
@Repository
public interface NotificationPresetRepository extends JpaRepository<NotificationPresetEntity, UUID> {

    // TODO(deferred): derived queries once the entity has fields.
}
