package de.vantrex.skysens.server.exception;

/**
 * Raised when {@code X-Skysens-Api-Version} falls outside the supported window.
 * Mapped to {@code 426 UPGRADE_REQUIRED} so the mod can distinguish "you are too
 * old" from an ordinary outage and stay silently local instead of retrying.
 */
public class UnsupportedApiVersionException extends SkysensApiException {

    public UnsupportedApiVersionException(final String message) {
        super("VERSION_UNSUPPORTED", message);
    }
}
