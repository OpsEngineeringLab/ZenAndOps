package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;

import java.time.Instant;

/**
 * Response DTO representing a Service.
 *
 * @param id             the service identifier
 * @param name           the service name
 * @param description    the service description
 * @param type           the service type
 * @param parentId       the parent service ID (null for root-level services)
 * @param organizationId the owning organization ID
 * @param businessOwner  the business owner
 * @param technicalOwner the technical owner
 * @param criticality    the criticality level
 * @param status         the service status
 * @param createdAt      when the service was created
 * @param updatedAt      when the service was last updated
 */
public record ServiceResponse(
        String id,
        String name,
        String description,
        ServiceType type,
        String parentId,
        String organizationId,
        String businessOwner,
        String technicalOwner,
        CriticalityLevel criticality,
        ServiceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
