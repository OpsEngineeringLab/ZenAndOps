package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.DataSourceType;

import java.time.Instant;

/**
 * Response DTO representing a DataSource.
 *
 * @param id                the data source identifier
 * @param name              the data source name
 * @param type              the data source type
 * @param reliabilityRating the reliability rating (0-100)
 * @param createdAt         when the data source was created
 * @param updatedAt         when the data source was last updated
 */
public record DataSourceResponse(
        String id,
        String name,
        DataSourceType type,
        int reliabilityRating,
        Instant createdAt,
        Instant updatedAt
) {
}
