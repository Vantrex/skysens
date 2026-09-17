package de.vantrex.skysens.client.net;

import de.vantrex.skysens.common.version.ApiVersion;

/**
 * Path constants, derived from {@link ApiVersion#PATH_SEGMENT} so bumping the API
 * version cannot leave a stale path behind. Mirrors the endpoint table in
 * ARCHITECTURE.md §7.
 */
public final class ApiEndpoints {

    private static final String BASE = "/api/" + ApiVersion.PATH_SEGMENT;

    // --- anonymous surface ---
    public static final String MANIFEST = BASE + "/manifest";
    public static final String LOCATION_ASSET = BASE + "/assets/locations";
    public static final String LOCATION_ASSET_META = BASE + "/assets/locations/meta";
    public static final String SPLIT_DEFINITIONS = BASE + "/splits/definitions";

    // --- bootstrap ---
    public static final String AUTH_HANDSHAKE = BASE + "/auth/handshake";
    public static final String AUTH_HANDSHAKE_COMPLETE = BASE + "/auth/handshake/complete";
    public static final String AUTH_REFRESH = BASE + "/auth/refresh";
    public static final String AUTH_ACCOUNT = BASE + "/auth/account";

    // --- authenticated ---
    public static final String RUNS = BASE + "/splits/runs";
    public static final String PERSONAL_BESTS = BASE + "/splits/personal-bests";
    public static final String SENSITIVITY_PROFILES = BASE + "/profiles/sensitivity";
    public static final String WAYPOINTS = BASE + "/waypoints";
    public static final String NOTIFICATION_PRESETS = BASE + "/presets/notifications";

    private ApiEndpoints() {
    }
}
