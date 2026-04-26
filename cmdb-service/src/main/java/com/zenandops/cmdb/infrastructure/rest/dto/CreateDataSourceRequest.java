package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.DataSourceType;

/**
 * Request DTO for creating a new DataSource.
 *
 * @param name              the data source name
 * @param type              the data source type (API, AGENT, FILE)
 * @param reliabilityRating the reliability rating (0-100)
 */
public record CreateDataSourceRequest(
        String name,
        DataSourceType type,
        int reliabilityRating
) {
}
