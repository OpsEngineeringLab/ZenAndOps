package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for assigning or removing Tags from a User.
 *
 * @param tagIds the list of tag identifiers
 */
public record UserTagsRequest(List<String> tagIds) {
}
