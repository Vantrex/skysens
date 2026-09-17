package de.vantrex.skysens.common.dto.common;

import java.time.Instant;
import java.util.Map;

/**
 * The single error envelope returned by every non-2xx response.
 *
 * @param code       stable machine-readable code, e.g. {@code VERSION_UNSUPPORTED}
 * @param message    human-readable, safe to log; never contains user data
 * @param path       request path that produced the error
 * @param timestamp  server-side instant the error was produced
 * @param details    optional field-level validation details, keyed by field name
 */
public record ApiError(
        String code,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> details
) {
}
