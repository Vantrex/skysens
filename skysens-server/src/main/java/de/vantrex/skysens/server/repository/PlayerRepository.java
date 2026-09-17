package de.vantrex.skysens.server.repository;

import de.vantrex.skysens.server.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Persistence for {@link PlayerEntity}.
 */
@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    // TODO(deferred): derived queries once the entity has fields.
}
