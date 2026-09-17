package de.vantrex.skysens.server.exception;

public class NotFoundException extends SkysensApiException {

    public NotFoundException(final String message) {
        super("NOT_FOUND", message);
    }
}
