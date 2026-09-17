package de.vantrex.skysens.server.security;

import org.springframework.context.annotation.Configuration;

/**
 * Route authorization. The split is deliberate and load-bearing for the
 * offline-first requirement:
 *
 * <ul>
 *   <li><b>Anonymous:</b> {@code /api/v1/manifest}, {@code /api/v1/assets/**},
 *       {@code /api/v1/splits/definitions}, and the whole of {@code /api/v1/auth}.
 *       These are reference data and the bootstrap path.</li>
 *   <li><b>Authenticated:</b> everything that reads or writes user data — run
 *       submission, personal bests, publishing, deletion.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    // TODO(deferred): SecurityFilterChain bean wiring SessionTokenFilter and the
    //                 anonymous/authenticated route split described above.
}
