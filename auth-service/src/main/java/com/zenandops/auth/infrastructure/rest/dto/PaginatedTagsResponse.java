package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Response DTO for a paginated list of Tags.
 *
 * @param items      the tags on the current page
 * @param page       the current page number (zero-based)
 * @param size       the page size
 * @param totalItems the total number of tags
 * @param totalPages the total number of pages
 */
public record PaginatedTagsResponse(List<TagResponse> items, int page, int size,
                                     long totalItems, int totalPages) {
}
