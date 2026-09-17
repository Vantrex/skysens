package de.vantrex.skysens.client.net;

import de.vantrex.skysens.common.version.ApiVersion;

/**
 * Path constants, derived from {@link ApiVersion#PATH_SEGMENT} so bumping the API
 * version cannot leave a stale path behind.
 *
 * <p>Only the auth handshake exists so far. Add a constant here when you add the
 * matching controller on the server — keeping paths in one place is the point.
 */
public final class ApiEndpoints {

    private static final String BASE = "/api/" + ApiVersion.PATH_SEGMENT;

    public static final String AUTH_HANDSHAKE = BASE + "/auth/handshake";
    public static final String AUTH_HANDSHAKE_COMPLETE = BASE + "/auth/handshake/complete";
    public static final String AUTH_REFRESH = BASE + "/auth/refresh";
    public static final String AUTH_ACCOUNT = BASE + "/auth/account";

    private ApiEndpoints() {
    }
}
