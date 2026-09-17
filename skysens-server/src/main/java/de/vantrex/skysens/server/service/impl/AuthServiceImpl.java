package de.vantrex.skysens.server.service.impl;

import de.vantrex.skysens.common.dto.auth.AuthHandshakeCompleteRequest;
import de.vantrex.skysens.common.dto.auth.AuthHandshakeStartResponse;
import de.vantrex.skysens.common.dto.auth.SessionTokenDto;
import de.vantrex.skysens.server.service.AuthService;
import org.springframework.stereotype.Service;

/**
 * Stub implementation. Every method body is deliberately unimplemented: this pass
 * produces structure only, and zero features are migrated to the server.
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public AuthHandshakeStartResponse startHandshake() {
        // TODO(deferred): generate a random serverId, store it with a short TTL
        throw new UnsupportedOperationException("TODO(deferred): handshake start");
    }

    @Override
    public SessionTokenDto completeHandshake(final AuthHandshakeCompleteRequest request) {
        // TODO(deferred): call Mojang sessionserver hasJoined?username=..&serverId=..
        //                 and take the UUID from ITS response, not from the request
        throw new UnsupportedOperationException("TODO(deferred): handshake completion");
    }

    @Override
    public SessionTokenDto refresh(final String refreshToken) {
        // TODO(deferred): validate, rotate (single-use), issue a new access token
        throw new UnsupportedOperationException("TODO(deferred): token refresh");
    }

    @Override
    public void deleteAccount(final java.util.UUID playerUuid) {
        // TODO(deferred): cascade-delete all data keyed to this UUID (§8 GDPR)
        throw new UnsupportedOperationException("TODO(deferred): account deletion");
    }
}
