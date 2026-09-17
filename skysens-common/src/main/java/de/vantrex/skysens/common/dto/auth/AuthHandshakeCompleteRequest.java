package de.vantrex.skysens.common.dto.auth;

/**
 * Step 2 of the handshake: the client tells the server it has called
 * {@code joinServer}, and the server verifies via Mojang {@code hasJoined}.
 *
 * <p>The username is a <em>lookup hint only</em> — the canonical identity is the
 * UUID Mojang returns from {@code hasJoined}, never anything the client asserts.
 */
public record AuthHandshakeCompleteRequest(
        String serverId,
        String username
) {
}
