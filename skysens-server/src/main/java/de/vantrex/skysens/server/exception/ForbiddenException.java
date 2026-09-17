package de.vantrex.skysens.server.exception;

public class ForbiddenException extends SkysensApiException {

    public ForbiddenException(final String message) {
        super("FORBIDDEN", message);
    }
}
