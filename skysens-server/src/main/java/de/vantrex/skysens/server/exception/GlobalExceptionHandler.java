package de.vantrex.skysens.server.exception;

import de.vantrex.skysens.common.dto.common.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Single exit point for errors. Every non-2xx response carries the same
 * {@link ApiError} envelope so the mod needs exactly one parse path.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(final NotFoundException exception) {
        // TODO(deferred): build the ApiError envelope (code, message, path, timestamp)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(final ForbiddenException exception) {
        // TODO(deferred): build the ApiError envelope
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(UnsupportedApiVersionException.class)
    public ResponseEntity<ApiError> handleUnsupportedVersion(final UnsupportedApiVersionException exception) {
        // TODO(deferred): build the ApiError envelope
        return ResponseEntity.status(HttpStatus.UPGRADE_REQUIRED).build();
    }

    @ExceptionHandler(SkysensApiException.class)
    public ResponseEntity<ApiError> handleApiException(final SkysensApiException exception) {
        // TODO(deferred): build the ApiError envelope
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
