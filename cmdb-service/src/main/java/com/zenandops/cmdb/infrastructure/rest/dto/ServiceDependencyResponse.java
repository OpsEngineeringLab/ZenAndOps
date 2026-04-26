package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.DependencyType;

import java.time.Instant;

/**
 * Response DTO representing a ServiceDependency.
 *
 * @param id              the dependency identifier
 * @param sourceServiceId the source service identifier
 * @param targetServiceId the target service identifier
 * @param dependencyType  the dependency type
 * @param createdAt       when the dependency was created
 */
public record ServiceDependencyResponse(
        String id,
        String sourceServiceId,
        String targetServiceId,
        DependencyType dependencyType,
        Instant createdAt
) {
}
