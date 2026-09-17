package de.vantrex.skysens.common.api;

import de.vantrex.skysens.common.dto.auth.AuthHandshakeCompleteRequest;
import de.vantrex.skysens.common.dto.auth.AuthHandshakeStartResponse;
import de.vantrex.skysens.common.dto.auth.RefreshTokenRequest;
import de.vantrex.skysens.common.dto.auth.SessionTokenDto;

/**
 * §6 — the Mojang session-server handshake. Unauthenticated by definition; this is
 * how a caller acquires the token every other API requires.
 */
public interface AuthApi {

    AuthHandshakeStartResponse startHandshake();

    SessionTokenDto completeHandshake(AuthHandshakeCompleteRequest request);

    SessionTokenDto refresh(RefreshTokenRequest request);

    /** GDPR deletion path (§8): erases everything keyed to the caller's UUID. */
    void deleteAccount();
}
