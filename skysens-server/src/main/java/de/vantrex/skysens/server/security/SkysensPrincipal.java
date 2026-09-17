package de.vantrex.skysens.server.security;

import java.util.UUID;

/**
 * The authenticated caller. There is exactly one identity in this system and it is
 * the Mojang UUID.
 *
 * <p>{@code displayName} is carried for convenience only. It is mutable and
 * trivially spoofable, so no authorization decision may ever read it.
 */
public record SkysensPrincipal(UUID playerUuid, String displayName) {
}
