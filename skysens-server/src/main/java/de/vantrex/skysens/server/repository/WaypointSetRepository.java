package de.vantrex.skysens.server.repository;

import de.vantrex.skysens.server.entity.WaypointSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Persistence for {@link WaypointSetEntity}.
 */
@Repository
public interface WaypointSetRepository extends JpaRepository<WaypointSetEntity, UUID> {

    // TODO(deferred): derived queries once the entity has fields.
}
