package com.zenandops.cmdb.infrastructure.rest.dto;

/**
 * Request DTO for creating a new ServiceCI association.
 *
 * @param serviceId the service identifier
 * @param ciId      the CI identifier
 */
public record CreateServiceCIRequest(
        String serviceId,
        String ciId
) {
}
