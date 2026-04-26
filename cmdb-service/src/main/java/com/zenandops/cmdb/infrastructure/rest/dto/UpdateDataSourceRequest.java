package com.zenandops.cmdb.infrastructure.rest.dto;

/**
 * Request DTO for updating an existing DataSource.
 *
 * @param name              the new data source name
 * @param reliabilityRating the new reliability rating (0-100)
 */
public record UpdateDataSourceRequest(
        String name,
        int reliabilityRating
) {
}
