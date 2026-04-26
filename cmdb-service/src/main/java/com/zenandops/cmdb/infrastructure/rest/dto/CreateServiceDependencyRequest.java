package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.DependencyType;

/**
 * Request DTO for creating a new ServiceDependency.
 *
 * @param sourceServiceId the source service identifier
 * @param targetServiceId the target service identifier
 * @param dependencyType  the dependency type (SYNCHRONOUS, ASYNCHRONOUS, CRITICAL)
 */
public record CreateServiceDependencyRequest(
        String sourceServiceId,
        String targetServiceId,
        DependencyType dependencyType
) {
}
