package com.zenandops.cmdb.infrastructure.rest.dto;

import java.time.Instant;

/**
 * Response DTO representing a ServiceCI association.
 *
 * @param id        the association identifier
 * @param serviceId the service identifier
 * @param ciId      the CI identifier
 * @param createdAt when the association was created
 */
public record ServiceCIResponse(
        String id,
        String serviceId,
        String ciId,
        Instant createdAt
) {
}
