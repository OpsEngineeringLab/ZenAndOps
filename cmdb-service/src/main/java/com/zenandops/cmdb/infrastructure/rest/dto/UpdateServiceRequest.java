package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;

/**
 * Request DTO for updating an existing Service.
 *
 * @param name           the new service name
 * @param description    the new service description
 * @param businessOwner  the new business owner
 * @param technicalOwner the new technical owner
 * @param criticality    the new criticality level
 * @param status         the new service status
 */
public record UpdateServiceRequest(
        String name,
        String description,
        String businessOwner,
        String technicalOwner,
        CriticalityLevel criticality,
        ServiceStatus status
) {
}
