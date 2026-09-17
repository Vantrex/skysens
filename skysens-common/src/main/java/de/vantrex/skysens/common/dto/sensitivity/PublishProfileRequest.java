package de.vantrex.skysens.common.dto.sensitivity;

import de.vantrex.skysens.common.domain.sensitivity.SensitivityConfiguration;

/**
 * Write side of §5.2. The author is taken from the bearer token, not the body.
 */
public record PublishProfileRequest(
        String name,
        String description,
        SensitivityConfiguration configuration
) {
}
