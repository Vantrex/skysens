package de.vantrex.skysens.common.dto.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * A short-lived Skysens session token plus the refresh token that renews it.
 *
 * @param accessToken   bearer token; minutes-to-an-hour lifetime
 * @param refreshToken  longer-lived, single-use, rotated on every refresh
 * @param playerUuid    canonical user key, straight from Mojang {@code hasJoined}
 * @param expiresAt     access token expiry
 */
public record SessionTokenDto(
        String accessToken,
        String refreshToken,
        UUID playerUuid,
        Instant expiresAt
) {
}
