package de.vantrex.skysens.server.controller;

import de.vantrex.skysens.common.dto.auth.AuthHandshakeCompleteRequest;
import de.vantrex.skysens.common.dto.auth.AuthHandshakeStartResponse;
import de.vantrex.skysens.common.dto.auth.RefreshTokenRequest;
import de.vantrex.skysens.common.dto.auth.SessionTokenDto;
import de.vantrex.skysens.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * §6 — Mojang session-server handshake. See ARCHITECTURE.md for the full flow.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/handshake")
    public AuthHandshakeStartResponse startHandshake() {
        // TODO(deferred): mint a one-shot serverId challenge and stash it with a TTL
        throw new UnsupportedOperationException("TODO(deferred): handshake start");
    }

    @PostMapping("/handshake/complete")
    public SessionTokenDto completeHandshake(@Valid @RequestBody final AuthHandshakeCompleteRequest request) {
        // TODO(deferred): verify via Mojang hasJoined, then mint a session token
        //                 keyed to the UUID Mojang returns (never the supplied name)
        throw new UnsupportedOperationException("TODO(deferred): handshake completion");
    }

    @PostMapping("/refresh")
    public SessionTokenDto refresh(@Valid @RequestBody final RefreshTokenRequest request) {
        // TODO(deferred): rotate the refresh token and issue a fresh access token
        throw new UnsupportedOperationException("TODO(deferred): token refresh");
    }

    @DeleteMapping("/account")
    public void deleteAccount() {
        // TODO(deferred): GDPR erasure (§8) — delete runs, PBs, published profiles
        //                 and presets for the caller's UUID, then revoke all tokens
        throw new UnsupportedOperationException("TODO(deferred): account deletion");
    }
}
