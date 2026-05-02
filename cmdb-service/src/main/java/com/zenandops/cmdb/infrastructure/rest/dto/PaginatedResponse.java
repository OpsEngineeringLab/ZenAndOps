package com.zenandops.cmdb.infrastructure.rest.dto;

import java.util.List;

/**
 * Generic paginated response wrapper for list endpoints.
 *
 * @param items      the items on the current page
 * @param page       the current page number (zero-based)
 * @param size       the requested page size
 * @param totalItems the total number of items across all pages
 * @param totalPages the total number of pages
 * @param <T>        the type of items in the response
 */
public record PaginatedResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalItems,
    int totalPages
) {

    /**
     * Creates a PaginatedResponse, calculating totalPages from totalItems and size.
     *
     * @param items      the items on the current page
     * @param page       the current page number (zero-based)
     * @param size       the requested page size
     * @param totalItems the total number of items across all pages
     * @param <T>        the type of items in the response
     * @return a new PaginatedResponse with totalPages calculated as ceil(totalItems / size)
     */
    public static <T> PaginatedResponse<T> of(List<T> items, int page, int size, long totalItems) {
        int totalPages = (int) Math.ceil((double) totalItems / size);
        return new PaginatedResponse<>(items, page, size, totalItems, totalPages);
    }
}
