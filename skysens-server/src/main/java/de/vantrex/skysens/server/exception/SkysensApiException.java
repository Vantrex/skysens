package de.vantrex.skysens.server.exception;

import lombok.Getter;

/**
 * Base for every error this API deliberately produces. Carries the stable machine
 * code that ends up in {@link de.vantrex.skysens.common.dto.common.ApiError}.
 */
@Getter
public class SkysensApiException extends RuntimeException {

    private final String code;

    public SkysensApiException(final String code, final String message) {
        super(message);
        this.code = code;
    }
}
