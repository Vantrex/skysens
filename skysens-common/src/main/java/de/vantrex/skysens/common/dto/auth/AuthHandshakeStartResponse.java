package de.vantrex.skysens.common.dto.auth;

/**
 * Step 1 of the Mojang session-server handshake (§6).
 *
 * <p>The server mints a one-shot {@code serverId} and the client calls Mojang's
 * {@code joinServer} with it. The nonce is opaque to the client.
 *
 * @param serverId       the hash the client must pass to {@code joinServer}
 * @param expiresInSeconds how long the server will remember this challenge
 */
public record AuthHandshakeStartResponse(
        String serverId,
        long expiresInSeconds
) {
}
