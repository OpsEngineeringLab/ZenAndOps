package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.DataOrigin;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO representing a CIVersion.
 *
 * @param id              the version identifier
 * @param ciId            the CI identifier
 * @param versionNumber   the sequential version number
 * @param attributes      the version attributes
 * @param startDate       when this version became active
 * @param endDate         when this version was superseded (null if current)
 * @param dataOrigin      the data origin
 * @param dataSourceId    the data source identifier
 * @param changeReference optional change reference
 * @param createdAt       when the version was created
 */
public record CIVersionResponse(
        String id,
        String ciId,
        int versionNumber,
        Map<String, Object> attributes,
        Instant startDate,
        Instant endDate,
        DataOrigin dataOrigin,
        String dataSourceId,
        String changeReference,
        Instant createdAt
) {
}
