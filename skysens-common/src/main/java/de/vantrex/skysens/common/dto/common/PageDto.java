package de.vantrex.skysens.common.dto.common;

import java.util.List;

/**
 * Cursor-free page envelope for every browse endpoint.
 */
public record PageDto<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
