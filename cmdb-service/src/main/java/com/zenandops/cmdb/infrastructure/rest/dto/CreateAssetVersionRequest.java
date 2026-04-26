package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.DataOrigin;

import java.util.Map;

/**
 * Request DTO for creating a new AssetVersion.
 *
 * @param description     the version description
 * @param attributes      the version attributes (JSON map)
 * @param dataOrigin      the data origin (API, AGENT, FILE)
 * @param dataSourceId    the data source identifier
 * @param changeReference optional change reference
 */
public record CreateAssetVersionRequest(
        String description,
        Map<String, Object> attributes,
        DataOrigin dataOrigin,
        String dataSourceId,
        String changeReference
) {
}
