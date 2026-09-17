package de.vantrex.skysens.common.dto.sensitivity;

import de.vantrex.skysens.common.domain.sensitivity.SensitivityConfiguration;

import java.time.Instant;
import java.util.UUID;

/**
 * A publishable sensitivity profile (§5.2) — the most naturally shareable thing
 * in the mod.
 *
 * <p>{@code configuration} is the very same type the mod persists locally, which
 * is exactly why it lives in {@code skysens-common}: publish and import are then
 * lossless by construction with no mapping layer to drift.
 *
 * @param authorName display name shown in the browse list; NOT an identity, and
 *                   never used for authorization
 */
public record SensitivityProfileDto(
        UUID profileId,
        String name,
        String description,
        UUID authorUuid,
        String authorName,
        Instant publishedAt,
        int downloadCount,
        SensitivityConfiguration configuration
) {
}
