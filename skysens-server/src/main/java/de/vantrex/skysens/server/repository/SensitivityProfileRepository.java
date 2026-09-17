package de.vantrex.skysens.server.repository;

import de.vantrex.skysens.server.entity.SensitivityProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Persistence for {@link SensitivityProfileEntity}.
 */
@Repository
public interface SensitivityProfileRepository extends JpaRepository<SensitivityProfileEntity, UUID> {

    // TODO(deferred): derived queries once the entity has fields.
}
