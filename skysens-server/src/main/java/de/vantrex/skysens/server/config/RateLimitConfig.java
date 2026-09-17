package de.vantrex.skysens.server.config;

import org.springframework.context.annotation.Configuration;

/**
 * Rate-limit buckets. See ARCHITECTURE.md §6 for the per-surface budgets.
 */
@Configuration
public class RateLimitConfig {

    // TODO(deferred): token-bucket filter keyed by UUID for authenticated routes
    // and by source IP for the anonymous surface.
}
