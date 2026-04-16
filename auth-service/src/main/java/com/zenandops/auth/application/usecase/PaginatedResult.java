package com.zenandops.auth.application.usecase;

import java.util.List;

/**
 * Generic paginated result for list operations.
 *
 * @param items      the items on the current page
 * @param page       the current page number (zero-based)
 * @param size       the page size
 * @param totalItems the total number of items across all pages
 * @param totalPages the total number of pages
 * @param <T>        the type of items
 */
public record PaginatedResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
}
