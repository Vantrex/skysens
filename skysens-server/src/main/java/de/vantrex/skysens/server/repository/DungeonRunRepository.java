package de.vantrex.skysens.server.repository;

import de.vantrex.skysens.server.entity.DungeonRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Persistence for {@link DungeonRunEntity}.
 */
@Repository
public interface DungeonRunRepository extends JpaRepository<DungeonRunEntity, UUID> {

    // TODO(deferred): derived queries once the entity has fields.
}
