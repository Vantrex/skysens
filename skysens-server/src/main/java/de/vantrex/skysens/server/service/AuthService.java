package de.vantrex.skysens.server.service;

import de.vantrex.skysens.common.dto.auth.AuthHandshakeCompleteRequest;
import de.vantrex.skysens.common.dto.auth.AuthHandshakeStartResponse;
import de.vantrex.skysens.common.dto.auth.SessionTokenDto;

import java.util.UUID;

public interface AuthService {

    AuthHandshakeStartResponse startHandshake();

    SessionTokenDto completeHandshake(AuthHandshakeCompleteRequest request);

    SessionTokenDto refresh(String refreshToken);

    void deleteAccount(UUID playerUuid);
}
