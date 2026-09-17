package de.vantrex.skysens.server.security;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class MojangSessionVerifierImpl implements MojangSessionVerifier {

    @Override
    public Optional<UUID> verify(final String username, final String serverId) {
        // TODO(deferred): HTTP GET sessionserver hasJoined; treat any non-200 as a
        //                 failed verification, never as a fallback to trusting the
        //                 supplied username.
        throw new UnsupportedOperationException("TODO(deferred): Mojang hasJoined verification");
    }
}
