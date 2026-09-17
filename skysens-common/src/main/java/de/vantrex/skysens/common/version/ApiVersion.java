package de.vantrex.skysens.common.version;

/**
 * The single source of truth for the Skysens HTTP API version.
 *
 * <p>Both sides compile against this constant, which is what makes the contract
 * impossible to drift: the mod sends {@link #HEADER} on every request and the
 * server rejects or degrades anything it cannot serve.
 */
public final class ApiVersion {

    /** Path segment used by every versioned endpoint, e.g. {@code /api/v1/...}. */
    public static final String PATH_SEGMENT = "v1";

    /** Request header carrying the client's API version. */
    public static final String HEADER = "X-Skysens-Api-Version";

    /** The API version this build of common speaks. */
    public static final int CURRENT = 1;

    /**
     * Oldest API version this build still understands. A peer reporting anything
     * below this is incompatible and must fall back to purely local behaviour.
     */
    public static final int MINIMUM_SUPPORTED = 1;

    private ApiVersion() {
    }

    /**
     * @return whether {@code peerVersion} can be talked to by this build.
     */
    public static boolean isCompatible(final int peerVersion) {
        return peerVersion >= MINIMUM_SUPPORTED && peerVersion <= CURRENT;
    }
}
