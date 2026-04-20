package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Response DTO for a paginated list of Users.
 *
 * @param items      the users on the current page
 * @param page       the current page number (zero-based)
 * @param size       the page size
 * @param totalItems the total number of users
 * @param totalPages the total number of pages
 */
public record PaginatedUsersResponse(List<UserResponse> items, int page, int size,
                                      long totalItems, int totalPages) {
}
