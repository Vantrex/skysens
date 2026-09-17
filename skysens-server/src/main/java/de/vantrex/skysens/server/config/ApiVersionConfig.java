package de.vantrex.skysens.server.config;

import org.springframework.context.annotation.Configuration;

/**
 * Enforcement point for {@link de.vantrex.skysens.common.version.ApiVersion}.
 *
 * <p>Registers the interceptor that reads {@code X-Skysens-Api-Version} off every
 * request and rejects versions outside the supported window with
 * {@code 426 UPGRADE_REQUIRED}.
 */
@Configuration
public class ApiVersionConfig {

    // TODO(deferred): register a HandlerInterceptor that validates the
    // X-Skysens-Api-Version header and short-circuits incompatible clients.
}
