package com.zenandops.cmdb.application.dto;

import java.util.List;

/**
 * Application-layer result object for paginated queries.
 * Contains the paginated items and the total count, allowing the REST layer
 * to build the full PaginatedResponse with page metadata.
 *
 * @param <T>        the entity type
 * @param items      the paginated items for the requested page
 * @param totalItems the total number of items matching the query filters
 */
public record PaginatedResult<T>(List<T> items, long totalItems) {
}
