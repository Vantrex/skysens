package de.vantrex.skysens.server.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Server half of the Mojang session-server handshake (§6).
 *
 * <p>Calls {@code https://sessionserver.mojang.com/session/minecraft/hasJoined}
 * with the username and the {@code serverId} the client just used for
 * {@code joinServer}. A 200 proves the caller holds a valid Mojang access token
 * for that account; the UUID in the response is the canonical user key.
 *
 * <p>The client half lives in the mod at
 * {@code de.vantrex.skysens.client.net.SkysensSession}.
 */
public interface MojangSessionVerifier {

    /**
     * @return the verified UUID, or empty when Mojang did not confirm the join
     *         (204/404), the challenge expired, or the call failed
     */
    Optional<UUID> verify(String username, String serverId);
}
