package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;

/**
 * Request DTO for creating a new Service.
 *
 * @param name           the service name
 * @param description    the service description
 * @param type           the service type (DOMAIN, BUSINESS, TECHNICAL)
 * @param parentId       optional parent service ID
 * @param organizationId the owning organization ID
 * @param businessOwner  the business owner
 * @param technicalOwner the technical owner
 * @param criticality    the criticality level (LOW, MEDIUM, HIGH, CRITICAL)
 * @param status         the service status (ACTIVE, INACTIVE, DEPRECATED)
 */
public record CreateServiceRequest(
        String name,
        String description,
        ServiceType type,
        String parentId,
        String organizationId,
        String businessOwner,
        String technicalOwner,
        CriticalityLevel criticality,
        ServiceStatus status
) {
}
